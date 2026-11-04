package com.messagestudio.app.ai

enum class Language(val label: String, val promptName: String) {
    ENGLISH("English", "English"),
    URDU("Urdu", "Urdu written in Urdu script"),
    ROMAN_URDU("Roman Urdu", "Roman Urdu (Urdu written with English letters)"),
    MIXED("Mixed", "a natural mix of English and Roman Urdu")
}

enum class Tone(val label: String) {
    FRIENDLY("Friendly"),
    PROFESSIONAL("Professional"),
    FORMAL("Formal"),
    CASUAL("Casual"),
    FUNNY("Funny"),
    MOTIVATIONAL("Motivational")
}

enum class Category(val label: String) {
    GREETING("Greeting"),
    BIRTHDAY("Birthday"),
    BUSINESS("Business"),
    EDUCATION("Education"),
    REMINDER("Reminder"),
    FRIENDSHIP("Friendship"),
    CUSTOM("Custom")
}

enum class LengthOption(val label: String, val hint: String) {
    SHORT("Short", "one short sentence, under 20 words"),
    MEDIUM("Medium", "two to three sentences"),
    LONG("Long", "a short paragraph of four to six sentences")
}

enum class EmojiMode(val label: String) {
    OFF("Off"),
    MINIMAL("Minimal"),
    ENABLED("Enabled")
}

enum class ImproveAction(val label: String) {
    REWRITE("Rewrite"),
    FIX_GRAMMAR("Fix spelling & grammar"),
    TO_URDU("To Urdu"),
    TO_ROMAN_URDU("To Roman Urdu"),
    SHORTER("Shorter"),
    LONGER("Longer"),
    CHANGE_TONE("Change tone"),
    ADD_EMOJI("Add emojis"),
    REMOVE_EMOJI("Remove emojis"),
    CLEARER_POLITE("Clearer & more polite"),
    ALTERNATIVES("3 alternatives")
}

inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

data class GenerationConfig(
    val instruction: String,
    val language: Language,
    val tone: Tone,
    val category: Category,
    val quantity: Int,
    val length: LengthOption,
    /** Only set when the user switched personalization on. */
    val recipientName: String?,
    val emoji: EmojiMode,
    val customInstructions: String,
    /** Optional saved-template body used as a style guide. */
    val templateBody: String?
)

data class AiUsage(val inputTokens: Long = 0, val outputTokens: Long = 0)

enum class AiErrorKind { NOT_CONFIGURED, NETWORK, TIMEOUT, RATE_LIMIT, CREDITS, AUTH, SERVER, BAD_RESPONSE, UNSUPPORTED }

data class AiError(val kind: AiErrorKind, val message: String, val retryAfterSeconds: Int? = null)

sealed interface AiResult<out T> {
    data class Success<T>(val value: T, val usage: AiUsage = AiUsage()) : AiResult<T>
    data class Failure(val error: AiError) : AiResult<Nothing>
}

data class TemplateDraft(
    val name: String,
    val description: String,
    val instruction: String,
    val language: Language,
    val tone: Tone,
    val category: Category,
    val length: LengthOption,
    val emoji: EmojiMode,
    val customInstructions: String,
    val body: String
)

data class ChatTurn(val role: String, val text: String, val drafts: List<String> = emptyList())

data class ChatReply(val text: String, val drafts: List<String>)

object MessageValidator {
    const val MAX_LENGTH = 2000

    fun clean(raw: List<String>): List<String> =
        raw.map { it.trim() }.filter { it.isNotEmpty() && it.length <= MAX_LENGTH }

    /** Same words with different punctuation, spacing, case or emoji count as the same message. */
    fun key(text: String): String =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}
