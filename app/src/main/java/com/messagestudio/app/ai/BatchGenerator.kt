package com.messagestudio.app.ai

import com.messagestudio.app.data.MessageDao
import com.messagestudio.app.data.MessageEntity
import com.messagestudio.app.data.UsageDao
import com.messagestudio.app.data.UsageEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class GenProgress(
    val running: Boolean = false,
    val requested: Int = 0,
    val saved: Int = 0,
    val duplicatesRemoved: Int = 0,
    val invalidRemoved: Int = 0,
    val batchesDone: Int = 0,
    val batchesFailed: Int = 0,
    val finished: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null,
    val note: String? = null,
    /** Texts saved during this run, in the order they were saved. */
    val sample: List<String> = emptyList()
)

/**
 * Generates a large number of messages in small batches, removes duplicates, saves each valid
 * unique message to Room, and keeps asking for replacements until the target is reached or it
 * is clear that no more unique messages are coming. If the provider fails it says so: it never
 * pretends that messages came from the AI when they did not.
 */
class BatchGenerator(private val messageDao: MessageDao, private val usageDao: UsageDao) {

    fun run(
        provider: AiProvider,
        recipientId: Long,
        config: GenerationConfig,
        batchSize: Int,
        dedupe: Boolean
    ): Flow<GenProgress> = flow {
        val source = if (provider.isExternal) "AI" else "LOCAL"
        val target = config.quantity
        val existing = HashSet(messageDao.keysFor(recipientId))
        val savedTexts = ArrayList<String>()
        var p = GenProgress(running = true, requested = target)
        emit(p)

        val maxAttempts = (target + batchSize - 1) / batchSize * 3 + 3
        var attempts = 0
        var emptyBatchesInRow = 0
        var failuresInRow = 0
        var rateLimitRetries = 0

        while (p.saved < target && attempts < maxAttempts) {
            attempts++
            val need = minOf(batchSize, target - p.saved)
            val ask = need + maxOf(2, need / 5)
            val avoid = savedTexts.takeLast(15)

            when (val r = provider.generate(config, ask, avoid)) {
                is AiResult.Failure -> {
                    usageDao.insert(UsageEntity(kind = "GENERATE", success = false, error = r.error.kind.name))
                    when (r.error.kind) {
                        AiErrorKind.NOT_CONFIGURED, AiErrorKind.AUTH, AiErrorKind.CREDITS, AiErrorKind.UNSUPPORTED -> {
                            emit(p.copy(running = false, finished = true, error = r.error.message))
                            return@flow
                        }
                        AiErrorKind.RATE_LIMIT -> {
                            rateLimitRetries++
                            if (rateLimitRetries > 3) {
                                emit(p.copy(running = false, finished = true, error = r.error.message))
                                return@flow
                            }
                            p = p.copy(batchesFailed = p.batchesFailed + 1, note = "The AI service is rate limiting. Waiting, then trying again...")
                            emit(p)
                            val wait = (r.error.retryAfterSeconds ?: (5 * rateLimitRetries)).coerceIn(2, 60)
                            delay(wait * 1000L)
                            attempts-- // a rate-limit wait should not use up a real attempt
                            continue
                        }
                        else -> {
                            failuresInRow++
                            p = p.copy(batchesFailed = p.batchesFailed + 1)
                            if (failuresInRow >= 3) {
                                emit(p.copy(running = false, finished = true, error = r.error.message))
                                return@flow
                            }
                            emit(p)
                        }
                    }
                }

                is AiResult.Success -> {
                    failuresInRow = 0
                    rateLimitRetries = 0
                    val cleaned = MessageValidator.clean(r.value)
                    var invalid = r.value.size - cleaned.size
                    var duplicates = 0
                    var added = 0

                    for (text in cleaned) {
                        if (p.saved + added >= target) break
                        val baseKey = MessageValidator.key(text)
                        if (baseKey.isEmpty()) { invalid++; continue }
                        val key = if (dedupe) baseKey else baseKey + "#" + System.nanoTime()
                        if (dedupe && key in existing) { duplicates++; continue }
                        val id = messageDao.insertIgnore(
                            MessageEntity(recipientId = recipientId, text = text, dedupeKey = key, source = source)
                        )
                        if (id == -1L) {
                            duplicates++
                        } else {
                            existing.add(key)
                            savedTexts.add(text)
                            added++
                        }
                    }

                    usageDao.insert(
                        UsageEntity(
                            kind = "GENERATE",
                            success = true,
                            messages = cleaned.size,
                            inputTokens = r.usage.inputTokens,
                            outputTokens = r.usage.outputTokens
                        )
                    )
                    emptyBatchesInRow = if (added == 0) emptyBatchesInRow + 1 else 0
                    p = p.copy(
                        saved = p.saved + added,
                        duplicatesRemoved = p.duplicatesRemoved + duplicates,
                        invalidRemoved = p.invalidRemoved + invalid,
                        batchesDone = p.batchesDone + 1,
                        note = null,
                        sample = savedTexts.toList()
                    )
                    emit(p)
                    if (emptyBatchesInRow >= 3) break
                }
            }
        }

        val shortfall = target - p.saved
        val note = if (shortfall > 0) {
            "Only ${p.saved} of $target unique messages could be made. " +
                if (provider.isExternal) "The AI kept repeating itself; try a more specific prompt or different settings."
                else "The offline templates have a limited number of different messages."
        } else null
        emit(p.copy(running = false, finished = true, note = note))
    }
}
