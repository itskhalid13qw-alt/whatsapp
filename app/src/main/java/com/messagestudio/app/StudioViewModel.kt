package com.messagestudio.app

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.messagestudio.app.ai.AiProvider
import com.messagestudio.app.ai.AiResult
import com.messagestudio.app.ai.BackendAiProvider
import com.messagestudio.app.ai.BatchGenerator
import com.messagestudio.app.ai.ChatReply
import com.messagestudio.app.ai.ChatTurn
import com.messagestudio.app.ai.GenProgress
import com.messagestudio.app.ai.GenerationConfig
import com.messagestudio.app.ai.ImproveAction
import com.messagestudio.app.ai.Language
import com.messagestudio.app.ai.LocalTemplateGenerator
import com.messagestudio.app.ai.MessageValidator
import com.messagestudio.app.ai.TemplateDraft
import com.messagestudio.app.ai.Tone
import com.messagestudio.app.data.AppDatabase
import com.messagestudio.app.data.MessageEntity
import com.messagestudio.app.data.RecipientEntity
import com.messagestudio.app.data.SettingsStore
import com.messagestudio.app.data.TemplateEntity
import com.messagestudio.app.data.UsageEntity
import com.messagestudio.app.data.UsageTotals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    val settings = SettingsStore(app)
    private val backend = BackendAiProvider(settings)
    private val local = LocalTemplateGenerator()
    private val generator = BatchGenerator(db.messages(), db.usage())

    val recipients = db.recipients().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedRecipientId = MutableStateFlow<Long?>(null)
    val messages = selectedRecipientId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else db.messages().observeFor(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val templates = db.templates().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val usageTotals = db.usage().totals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageTotals(0, 0, 0, 0))
    val recentUsage = db.usage().recent(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var gen by mutableStateOf(GenProgress())
        private set
    var generatedToday by mutableIntStateOf(0)
        private set
    var pendingTemplate by mutableStateOf<TemplateEntity?>(null)
    /** One-time message shown in a snackbar. */
    var notice by mutableStateOf<String?>(null)

    val chat = mutableStateListOf<ChatTurn>()
    var chatBusy by mutableStateOf(false)
        private set
    var chatError by mutableStateOf<String?>(null)
        private set

    private var genJob: Job? = null

    init {
        viewModelScope.launch {
            recipients.collect { list ->
                val current = selectedRecipientId.value
                if (list.none { it.id == current }) selectedRecipientId.value = list.firstOrNull()?.id
            }
        }
        refreshToday()
    }

    // ----- recipients and messages -----

    fun selectRecipient(id: Long) { selectedRecipientId.value = id }

    fun addRecipient(name: String, phone: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val id = db.recipients().insert(RecipientEntity(name = cleanName, phone = phone.filter { it.isDigit() }))
            selectedRecipientId.value = id
        }
    }

    fun deleteRecipient(r: RecipientEntity) {
        viewModelScope.launch { db.recipients().delete(r) }
    }

    fun saveMessage(recipientId: Long, text: String, source: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            val key = MessageValidator.key(t).ifBlank { t }
            val id = db.messages().insertIgnore(MessageEntity(recipientId = recipientId, text = t, dedupeKey = key, source = source))
            notice = if (id == -1L) "That message already exists for this recipient." else "Message saved."
        }
    }

    fun updateMessage(m: MessageEntity, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        viewModelScope.launch {
            try {
                db.messages().updateText(m.id, t, MessageValidator.key(t).ifBlank { t })
            } catch (e: SQLiteConstraintException) {
                notice = "Another message for this recipient already has that text."
            }
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch { db.messages().delete(id) }
    }

    // ----- generation -----

    fun startGeneration(config: GenerationConfig, useLocal: Boolean, dedupe: Boolean) {
        val recipientId = selectedRecipientId.value
        if (recipientId == null) {
            notice = "Choose or add a recipient first."
            return
        }
        if (gen.running) return
        genJob = viewModelScope.launch {
            if (!useLocal && settings.dailyLimit > 0) {
                val used = db.usage().generatedSince(startOfToday())
                if (used + config.quantity > settings.dailyLimit) {
                    gen = GenProgress(
                        finished = true,
                        error = "Your daily limit is ${settings.dailyLimit} AI messages and $used were already used today. " +
                            "Ask for fewer messages, or change the limit in Settings."
                    )
                    return@launch
                }
            }
            val provider: AiProvider = if (useLocal) local else backend
            gen = GenProgress(running = true, requested = config.quantity)
            try {
                generator.run(provider, recipientId, config, settings.batchSize, dedupe).collect { gen = it }
            } catch (e: CancellationException) {
                gen = gen.copy(running = false, finished = true, cancelled = true)
                throw e
            } finally {
                refreshToday()
            }
        }
    }

    fun cancelGeneration() { genJob?.cancel() }

    fun dismissGenResult() { if (!gen.running) gen = GenProgress() }

    // ----- AI helpers -----

    suspend fun improve(text: String, action: ImproveAction, tone: Tone?): AiResult<List<String>> {
        val r = backend.improve(text, action, tone)
        record("IMPROVE", r)
        return r
    }

    suspend fun buildTemplate(description: String, language: Language): AiResult<TemplateDraft> {
        val r = backend.buildTemplate(description, language)
        record("TEMPLATE", r)
        return r
    }

    fun saveTemplate(t: TemplateEntity) {
        viewModelScope.launch { db.templates().upsert(t) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { db.templates().delete(id) }
    }

    fun sendChat(text: String) {
        val t = text.trim()
        if (t.isEmpty() || chatBusy) return
        chat.add(ChatTurn("user", t))
        chatBusy = true
        chatError = null
        viewModelScope.launch {
            val r: AiResult<ChatReply> = backend.chat(chat.toList())
            record("CHAT", r)
            when (r) {
                is AiResult.Success -> chat.add(ChatTurn("assistant", r.value.text, r.value.drafts))
                is AiResult.Failure -> chatError = r.error.message
            }
            chatBusy = false
        }
    }

    fun clearChat() { chat.clear(); chatError = null }

    fun clearUsage() {
        viewModelScope.launch { db.usage().clear(); refreshToday() }
    }

    // ----- helpers -----

    private suspend fun <T> record(kind: String, r: AiResult<T>) {
        when (r) {
            is AiResult.Success -> db.usage().insert(
                UsageEntity(kind = kind, success = true, inputTokens = r.usage.inputTokens, outputTokens = r.usage.outputTokens)
            )
            is AiResult.Failure -> db.usage().insert(UsageEntity(kind = kind, success = false, error = r.error.kind.name))
        }
    }

    private fun refreshToday() {
        viewModelScope.launch { generatedToday = db.usage().generatedSince(startOfToday()) }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
