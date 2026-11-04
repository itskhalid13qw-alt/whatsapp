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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messagestudio.app.StudioViewModel
import com.messagestudio.app.ai.AiResult
import com.messagestudio.app.ai.Category
import com.messagestudio.app.ai.EmojiMode
import com.messagestudio.app.ai.Language
import com.messagestudio.app.ai.LengthOption
import com.messagestudio.app.ai.Tone
import com.messagestudio.app.ai.enumOr
import com.messagestudio.app.data.TemplateEntity
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen(vm: StudioViewModel, requestAi: (() -> Unit) -> Unit, useTemplate: (TemplateEntity) -> Unit) {
    val templates by vm.templates.collectAsState()
    var dialogFor by remember { mutableStateOf<TemplateEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Templates", style = MaterialTheme.typography.headlineSmall)
                SmallNote("A template is a saved style (language, tone, length, instructions) you can reuse in AI Studio.")
                PrimaryButton("Create My AI Template") { creating = true }
            }
        }
        items(templates, key = { it.id }) { t ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t.name, style = MaterialTheme.typography.titleMedium)
                    SmallNote(
                        "${enumOr(t.language, Language.ENGLISH).label} - ${enumOr(t.tone, Tone.FRIENDLY).label} - " +
                            enumOr(t.category, Category.CUSTOM).label
                    )
                    if (t.instruction.isNotBlank()) Text(t.instruction)
                    if (t.body.isNotBlank()) SmallNote("Example: ${t.body}")
                    Row {
                        TextButton(onClick = { useTemplate(t) }) { Text("Use in AI Studio") }
                        TextButton(onClick = { dialogFor = t }) { Text("Edit") }
                        TextButton(onClick = { vm.deleteTemplate(t.id) }) { Text("Delete") }
                    }
                }
            }
        }
        if (templates.isEmpty()) {
            item { Text("No templates yet.") }
        }
    }

    if (creating) {
        TemplateDialog(vm = vm, requestAi = requestAi, existing = null, onDismiss = { creating = false })
    }
    dialogFor?.let { t ->
        TemplateDialog(vm = vm, requestAi = requestAi, existing = t, onDismiss = { dialogFor = null })
    }
}

@Composable
private fun TemplateDialog(
    vm: StudioViewModel,
    requestAi: (() -> Unit) -> Unit,
    existing: TemplateEntity?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var language by remember { mutableStateOf(enumOr(existing?.language, Language.ENGLISH)) }
    var tone by remember { mutableStateOf(enumOr(existing?.tone, Tone.FRIENDLY)) }
    var category by remember { mutableStateOf(enumOr(existing?.category, Category.CUSTOM)) }
    var length by remember { mutableStateOf(enumOr(existing?.length, LengthOption.MEDIUM)) }
    var emoji by remember { mutableStateOf(enumOr(existing?.emoji, EmojiMode.MINIMAL)) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var instruction by remember { mutableStateOf(existing?.instruction.orEmpty()) }
    var custom by remember { mutableStateOf(existing?.customInstructions.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var ready by remember { mutableStateOf(existing != null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generate() {
        if (description.isBlank()) {
            error = "Describe the style you want first."
            return
        }
        scope.launch {
            busy = true
            error = null
            when (val r = vm.buildTemplate(description.trim(), language)) {
                is AiResult.Success -> {
                    val d = r.value
                    name = d.name; instruction = d.instruction; custom = d.customInstructions; body = d.body
                    language = d.language; tone = d.tone; category = d.category; length = d.length; emoji = d.emoji
                    ready = true
                }
                is AiResult.Failure -> error = r.error.message
            }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create My AI Template" else "Edit template") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existing == null) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the style in your own words") },
                        placeholder = { Text("e.g. Polite Urdu messages reminding a friend about a meeting") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChipRow("Language", Language.values().toList(), language, { it.label }) { language = it }
                    Row {
                        PrimaryButton("Ask AI to build it", enabled = !busy) { requestAi { generate() } }
                        TextButton(onClick = { ready = true }, enabled = !busy) { Text("Fill in myself") }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    SmallNote("Asking the AI sends your description to your AI backend.")
                }
                if (ready) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = instruction, onValueChange = { instruction = it }, label = { Text("What the messages should say") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = custom, onValueChange = { custom = it }, label = { Text("Extra instructions") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Example message (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    ChipRow("Language", Language.values().toList(), language, { it.label }) { language = it }
                    Dropdown("Tone", tone.label, Tone.values().toList(), { it.label }, { tone = it }, Modifier.fillMaxWidth())
                    Dropdown("Category", category.label, Category.values().toList(), { it.label }, { category = it }, Modifier.fillMaxWidth())
                    ChipRow("Length", LengthOption.values().toList(), length, { it.label }) { length = it }
                    ChipRow("Emoji", EmojiMode.values().toList(), emoji, { it.label }) { emoji = it }
                    if (body.isNotBlank()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                SmallNote("Preview")
                                Text(body)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = ready && name.isNotBlank() && (instruction.isNotBlank() || body.isNotBlank()),
                onClick = {
                    vm.saveTemplate(
                        TemplateEntity(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            description = description.trim(),
                            instruction = instruction.trim(),
                            language = language.name,
                            tone = tone.name,
                            category = category.name,
                            length = length.name,
                            emoji = emoji.name,
                            customInstructions = custom.trim(),
                            body = body.trim(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
