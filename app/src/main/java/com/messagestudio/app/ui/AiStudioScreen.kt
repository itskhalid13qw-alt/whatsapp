package com.messagestudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messagestudio.app.StudioViewModel
import com.messagestudio.app.ai.Category
import com.messagestudio.app.ai.EmojiMode
import com.messagestudio.app.ai.GenerationConfig
import com.messagestudio.app.ai.Language
import com.messagestudio.app.ai.LengthOption
import com.messagestudio.app.ai.Tone
import com.messagestudio.app.ai.enumOr

private val QUANTITIES = listOf(10, 50, 100, 500, 1000)

@Composable
fun AiStudioScreen(vm: StudioViewModel, requestAi: (() -> Unit) -> Unit, goMessages: () -> Unit) {
    val recipients by vm.recipients.collectAsState()
    val selectedId by vm.selectedRecipientId.collectAsState()
    val selected = recipients.firstOrNull { it.id == selectedId }
    val gen = vm.gen

    var instruction by rememberSaveable { mutableStateOf("") }
    var customInstructions by rememberSaveable { mutableStateOf("") }
    var language by remember { mutableStateOf(Language.ENGLISH) }
    var tone by remember { mutableStateOf(Tone.FRIENDLY) }
    var category by remember { mutableStateOf(Category.GREETING) }
    var length by remember { mutableStateOf(LengthOption.SHORT) }
    var quantity by remember { mutableStateOf(10) }
    var emoji by remember { mutableStateOf(EmojiMode.MINIMAL) }
    var personalize by remember { mutableStateOf(false) }
    var dedupe by remember { mutableStateOf(true) }
    var templateBody by remember { mutableStateOf<String?>(null) }
    var templateName by remember { mutableStateOf<String?>(null) }
    var showAddRecipient by remember { mutableStateOf(false) }
    var largeRequestAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // A template chosen on the Templates tab fills in the form.
    val pending = vm.pendingTemplate
    LaunchedEffect(pending) {
        pending?.let { t ->
            instruction = t.instruction
            customInstructions = t.customInstructions
            language = enumOr(t.language, Language.ENGLISH)
            tone = enumOr(t.tone, Tone.FRIENDLY)
            category = enumOr(t.category, Category.CUSTOM)
            length = enumOr(t.length, LengthOption.MEDIUM)
            emoji = enumOr(t.emoji, EmojiMode.MINIMAL)
            templateBody = t.body.ifBlank { null }
            templateName = t.name
            vm.pendingTemplate = null
        }
    }

    fun buildConfig() = GenerationConfig(
        instruction = instruction.trim(),
        language = language,
        tone = tone,
        category = category,
        quantity = quantity,
        length = length,
        recipientName = if (personalize) selected?.name else null,
        emoji = emoji,
        customInstructions = customInstructions.trim(),
        templateBody = templateBody
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI Studio", style = MaterialTheme.typography.headlineSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Dropdown(
                        label = "Generate for",
                        selected = selected?.name ?: "No recipient yet",
                        options = recipients,
                        optionLabel = { it.name },
                        onSelect = { vm.selectRecipient(it.id) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showAddRecipient = true }) { Text("New") }
                }

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("What should the messages say?") },
                    placeholder = { Text("e.g. Polite Urdu reminders about a meeting") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                templateName?.let { name ->
                    Row {
                        SmallNote("Using your template: $name")
                        TextButton(onClick = { templateBody = null; templateName = null }) { Text("Stop using it") }
                    }
                }

                ChipRow("Language", Language.values().toList(), language, { it.label }) { language = it }
                Dropdown("Tone", tone.label, Tone.values().toList(), { it.label }, { tone = it }, Modifier.fillMaxWidth())
                Dropdown("Category", category.label, Category.values().toList(), { it.label }, { category = it }, Modifier.fillMaxWidth())
                ChipRow("Message length", LengthOption.values().toList(), length, { it.label }) { length = it }
                ChipRow("How many messages", QUANTITIES, quantity, { it.toString() }) { quantity = it }
                ChipRow("Emoji", EmojiMode.values().toList(), emoji, { it.label }) { emoji = it }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Use the recipient's name")
                        SmallNote("Off by default. If on, the name is sent to the AI service.")
                    }
                    Switch(checked = personalize, onCheckedChange = { personalize = it }, enabled = selected != null)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remove duplicates")
                        SmallNote("Same words with different punctuation or emoji count as duplicates.")
                    }
                    Switch(checked = dedupe, onCheckedChange = { dedupe = it })
                }

                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    label = { Text("Custom instructions (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                SmallNote(
                    "AI mode sends your prompt and these options to your AI backend. Provider usage may cost money, and the " +
                        "limits depend on the service you use. Generated today: ${vm.generatedToday}" +
                        if (vm.settings.dailyLimit > 0) " of ${vm.settings.dailyLimit}." else "."
                )

                PrimaryButton(
                    text = "Generate AI Messages",
                    enabled = !gen.running && selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (instruction.isBlank() && templateBody == null) {
                        vm.notice = "Describe what the messages should say first."
                    } else {
                        val start = { requestAi { vm.startGeneration(buildConfig(), useLocal = false, dedupe = dedupe) } }
                        if (quantity >= vm.settings.warnAt) largeRequestAction = start else start()
                    }
                }
                OutlinedButton(
                    onClick = { vm.startGeneration(buildConfig(), useLocal = true, dedupe = dedupe) },
                    enabled = !gen.running && selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Use offline templates (not AI)") }
                SmallNote(
                    "Offline templates work without internet and send nothing anywhere. They mix built-in sentences, ignore " +
                        "your prompt, and can only make a limited number of different messages."
                )

                if (gen.running) {
                    val fraction = if (gen.requested > 0) gen.saved.toFloat() / gen.requested else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Text("Saved ${gen.saved} of ${gen.requested} unique messages")
                    SmallNote("Duplicates removed: ${gen.duplicatesRemoved}   Failed batches: ${gen.batchesFailed}")
                    gen.note?.let { SmallNote(it) }
                    OutlinedButton(onClick = { vm.cancelGeneration() }) { Text("Cancel") }
                } else if (gen.finished) {
                    val headline = when {
                        gen.cancelled -> "Cancelled. ${gen.saved} unique messages were saved."
                        gen.error != null -> "Stopped. ${gen.saved} unique messages were saved."
                        else -> "Done. ${gen.saved} unique messages saved."
                    }
                    Text(headline, style = MaterialTheme.typography.titleMedium)
                    SmallNote("Duplicates removed: ${gen.duplicatesRemoved}   Invalid removed: ${gen.invalidRemoved}   Failed batches: ${gen.batchesFailed}")
                    gen.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    gen.note?.let { SmallNote(it) }
                    Row {
                        PrimaryButton("Review messages") { vm.dismissGenResult(); goMessages() }
                        TextButton(onClick = { vm.dismissGenResult() }) { Text("Dismiss") }
                    }
                }

                if (gen.sample.isNotEmpty()) {
                    Text("Latest generated (last ${minOf(30, gen.sample.size)} of ${gen.sample.size})", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        items(gen.sample.takeLast(30).reversed()) { text ->
            Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(12.dp)) }
        }
    }

    if (showAddRecipient) {
        AddRecipientDialog(onDismiss = { showAddRecipient = false }, onAdd = { n, p -> vm.addRecipient(n, p) })
    }

    largeRequestAction?.let { go ->
        val batches = (quantity + vm.settings.batchSize - 1) / vm.settings.batchSize
        AlertDialog(
            onDismissRequest = { largeRequestAction = null },
            title = { Text("Large request") },
            text = {
                Text(
                    "Asking for $quantity messages takes about $batches requests to your AI provider, and more if duplicates " +
                        "have to be replaced. Provider usage may cost money, and actual limits depend on your service. Continue?"
                )
            },
            confirmButton = { TextButton(onClick = { largeRequestAction = null; go() }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { largeRequestAction = null }) { Text("Cancel") } }
        )
    }
}
