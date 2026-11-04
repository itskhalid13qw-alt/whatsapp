package com.messagestudio.app.ai

import com.messagestudio.app.data.SettingsStore
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Talks to YOUR backend over HTTPS (see the /backend folder). The backend holds the AI provider's
 * secret key and decides which provider is used, so this app never contains a private key.
 */
class BackendAiProvider(private val settings: SettingsStore) : AiProvider {
    override val name = "AI backend"
    override val isExternal = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(config: GenerationConfig, count: Int, avoid: List<String>): AiResult<List<String>> {
        val body = JSONObject()
            .put("instruction", config.instruction)
            .put("language", config.language.name)
            .put("languageName", config.language.promptName)
            .put("tone", config.tone.label)
            .put("category", config.category.label)
            .put("length", config.length.hint)
            .put("emoji", config.emoji.name)
            .put("quantity", count)
            .put("customInstructions", config.customInstructions)
            .put("templateBody", config.templateBody.orEmpty())
            .put("avoid", JSONArray(avoid))
        if (!config.recipientName.isNullOrBlank()) body.put("recipientName", config.recipientName)

        return when (val r = post("/v1/generate", body)) {
            is AiResult.Failure -> r
            is AiResult.Success -> {
                val list = r.value.strings("messages") ?: return bad("The AI backend did not return a messages list.")
                AiResult.Success(MessageValidator.clean(list), r.usage)
            }
        }
    }

    override suspend fun improve(text: String, action: ImproveAction, tone: Tone?): AiResult<List<String>> {
        val body = JSONObject().put("text", text).put("action", action.name)
        if (tone != null) body.put("tone", tone.label)
        return when (val r = post("/v1/improve", body)) {
            is AiResult.Failure -> r
            is AiResult.Success -> {
                val list = MessageValidator.clean(r.value.strings("messages").orEmpty())
                if (list.isEmpty()) bad("The AI backend returned no text.") else AiResult.Success(list, r.usage)
            }
        }
    }

    override suspend fun buildTemplate(description: String, language: Language): AiResult<TemplateDraft> {
        val body = JSONObject().put("description", description).put("language", language.name)
        return when (val r = post("/v1/template", body)) {
            is AiResult.Failure -> r
            is AiResult.Success -> {
                val j = r.value
                val instruction = j.optString("instruction").trim()
                val sample = j.optString("body").trim()
                if (instruction.isEmpty() && sample.isEmpty()) return bad("The AI backend returned an empty template.")
                AiResult.Success(
                    TemplateDraft(
                        name = j.optString("name").trim().ifBlank { "My template" },
                        description = description,
                        instruction = instruction.ifBlank { description },
                        language = enumOr(j.optString("language"), language),
                        tone = enumOr(j.optString("tone"), Tone.FRIENDLY),
                        category = enumOr(j.optString("category"), Category.CUSTOM),
                        length = enumOr(j.optString("length"), LengthOption.MEDIUM),
                        emoji = enumOr(j.optString("emoji"), EmojiMode.MINIMAL),
                        customInstructions = j.optString("customInstructions").trim(),
                        body = sample
                    ),
                    r.usage
                )
            }
        }
    }

    override suspend fun chat(history: List<ChatTurn>): AiResult<ChatReply> {
        val turns = JSONArray()
        history.takeLast(20).forEach { turns.put(JSONObject().put("role", it.role).put("text", it.text)) }
        return when (val r = post("/v1/chat", JSONObject().put("messages", turns))) {
            is AiResult.Failure -> r
            is AiResult.Success -> {
                val reply = r.value.optString("reply").trim()
                val drafts = MessageValidator.clean(r.value.strings("drafts").orEmpty())
                if (reply.isEmpty() && drafts.isEmpty()) bad("The AI backend returned an empty reply.")
                else AiResult.Success(ChatReply(reply, drafts), r.usage)
            }
        }
    }

    // ----- networking -----

    private suspend fun post(path: String, body: JSONObject): AiResult<JSONObject> {
        val base = settings.backendUrl.trim().trimEnd('/')
        if (base.isEmpty()) {
            return fail(
                AiErrorKind.NOT_CONFIGURED,
                "No AI backend is set. Open Settings and enter your backend address, or use the offline template generator."
            )
        }
        if (!base.startsWith("https://")) {
            return fail(AiErrorKind.NOT_CONFIGURED, "The backend address must start with https:// so messages are sent securely.")
        }
        return try {
            val builder = Request.Builder()
                .url("$base$path")
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            if (settings.authToken.isNotBlank()) builder.header("Authorization", "Bearer ${settings.authToken}")
            client.newCall(builder.build()).await().use { response -> handle(response) }
        } catch (e: SocketTimeoutException) {
            fail(AiErrorKind.TIMEOUT, "The AI backend took too long to answer. Try again, or ask for fewer messages.")
        } catch (e: IOException) {
            fail(AiErrorKind.NETWORK, "Could not reach the AI backend. Check your internet connection and the backend address.")
        } catch (e: IllegalArgumentException) {
            fail(AiErrorKind.NOT_CONFIGURED, "The backend address is not valid.")
        }
    }

    private fun handle(response: Response): AiResult<JSONObject> {
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            return AiResult.Failure(errorFor(response.code, text, response.header("Retry-After")))
        }
        val json = try {
            JSONObject(text)
        } catch (e: JSONException) {
            return bad("The AI backend sent a reply the app could not read.")
        }
        val usage = json.optJSONObject("usage")?.let {
            AiUsage(it.optLong("inputTokens", 0), it.optLong("outputTokens", 0))
        } ?: AiUsage()
        return AiResult.Success(json, usage)
    }

    private fun errorFor(code: Int, text: String, retryAfter: String?): AiError {
        val serverMessage = try {
            JSONObject(text).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: JSONException) {
            ""
        }
        return when {
            code == 429 -> AiError(
                AiErrorKind.RATE_LIMIT,
                serverMessage.ifBlank { "The AI service is limiting requests right now. Wait a moment and try again." },
                retryAfter?.toIntOrNull()
            )
            code == 402 -> AiError(
                AiErrorKind.CREDITS,
                serverMessage.ifBlank { "The AI provider says the credits or quota for this account are used up." }
            )
            code == 401 || code == 403 -> AiError(
                AiErrorKind.AUTH,
                serverMessage.ifBlank { "The backend refused the access token. Check it in Settings." }
            )
            code in 500..599 -> AiError(
                AiErrorKind.SERVER,
                serverMessage.ifBlank { "The AI backend had a problem ($code). Try again later." }
            )
            else -> AiError(AiErrorKind.BAD_RESPONSE, serverMessage.ifBlank { "Unexpected reply from the AI backend ($code)." })
        }
    }

    private fun fail(kind: AiErrorKind, message: String) = AiResult.Failure(AiError(kind, message))
    private fun bad(message: String) = fail(AiErrorKind.BAD_RESPONSE, message)

    private fun JSONObject.strings(key: String): List<String>? {
        val arr = optJSONArray(key) ?: return null
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val v = arr.opt(i)
            if (v is String && v.isNotBlank()) out.add(v)
        }
        return out
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
    }
}
