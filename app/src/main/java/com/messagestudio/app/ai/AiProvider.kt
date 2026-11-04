package com.messagestudio.app.ai

/** Everything the app needs from an AI service. Swap the implementation to change provider. */
interface AiProvider {
    val name: String

    /** True when message text leaves the phone for this provider. */
    val isExternal: Boolean

    suspend fun generate(config: GenerationConfig, count: Int, avoid: List<String>): AiResult<List<String>>
    suspend fun improve(text: String, action: ImproveAction, tone: Tone?): AiResult<List<String>>
    suspend fun buildTemplate(description: String, language: Language): AiResult<TemplateDraft>
    suspend fun chat(history: List<ChatTurn>): AiResult<ChatReply>
}
