package com.messagestudio.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Offline fallback. It mixes built-in sentence templates, so it works with no internet, but it is
 * NOT AI: it ignores the prompt text, and the number of different messages it can make is limited.
 * Messages made here are always saved with the source "LOCAL" so they are labeled in the app.
 */
class LocalTemplateGenerator : AiProvider {
    override val name = "Offline templates"
    override val isExternal = false

    override suspend fun generate(config: GenerationConfig, count: Int, avoid: List<String>): AiResult<List<String>> =
        withContext(Dispatchers.Default) {
            val rnd = Random(System.nanoTime())
            val out = LinkedHashSet<String>()
            var guard = 0
            while (out.size < count && guard < count * 20) {
                guard++
                out.add(composeLocalMessage(config, rnd))
            }
            AiResult.Success(out.toList())
        }

    private fun unsupported(): AiResult.Failure =
        AiResult.Failure(AiError(AiErrorKind.UNSUPPORTED, "This needs an AI provider. Set up your AI backend in Settings."))

    override suspend fun improve(text: String, action: ImproveAction, tone: Tone?): AiResult<List<String>> = unsupported()
    override suspend fun buildTemplate(description: String, language: Language): AiResult<TemplateDraft> = unsupported()
    override suspend fun chat(history: List<ChatTurn>): AiResult<ChatReply> = unsupported()
}
