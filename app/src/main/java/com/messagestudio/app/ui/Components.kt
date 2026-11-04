package com.messagestudio.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.messagestudio.app.StudioViewModel
import com.messagestudio.app.ai.AiResult
import com.messagestudio.app.ai.ImproveAction
import com.messagestudio.app.ai.Tone
import com.messagestudio.app.data.RecipientEntity
import kotlinx.coroutines.launch

/** Opens WhatsApp with the text ready. The user still has to press Send themselves. */
fun shareToWhatsApp(context: Context, phone: String, text: String) {
    val digits = phone.filter { it.isDigit() }
    try {
        val intent = if (digits.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(text)}"))
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
            }
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Share message"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(
    label: String,
    selected: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun <T> ChipRow(
    label: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelOf(option)) }
                )
            }
        }
    }
}

@Composable
fun ConsentDialog(backendUrl: String, onAllow: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Send text to an AI service?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "AI features send what you type here (your instructions, the options you choose, messages you ask " +
                        "to improve, and assistant chat messages) over HTTPS to your AI backend:"
                )
                Text(backendUrl.ifBlank { "(no backend set yet; add it in Settings)" }, style = MaterialTheme.typography.labelLarge)
                Text("The backend passes it to an AI provider to write the reply.")
                Text(
                    "Your contacts, phone numbers and WhatsApp conversations are never sent. A recipient's name is sent only " +
                        "if you switch on personalization. Nothing is ever sent on WhatsApp automatically."
                )
                Text("AI providers can charge for usage, and their limits depend on the service you use.")
            }
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Not now") } }
    )
}

@Composable
fun AddRecipientDialog(onDismiss: () -> Unit, onAdd: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New recipient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("WhatsApp number (optional)") },
                    supportingText = { Text("With country code, e.g. 923001234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name, phone); onDismiss() }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Edit a message, with optional AI tools. The original text stays until the user accepts a change. */
@Composable
fun MessageEditorDialog(
    title: String,
    initial: String,
    vm: StudioViewModel,
    requestAi: (() -> Unit) -> Unit,
    recipients: List<RecipientEntity>?,
    defaultRecipientId: Long?,
    onDismiss: () -> Unit,
    onSave: (recipientId: Long?, text: String) -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var recipientId by remember { mutableStateOf(defaultRecipientId) }
    val canSave = draft.isNotBlank() && (recipients == null || recipientId != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (recipients != null) {
                    Dropdown(
                        label = "Save for",
                        selected = recipients.firstOrNull { it.id == recipientId }?.name ?: "Choose a recipient",
                        options = recipients,
                        optionLabel = { it.name },
                        onSelect = { recipientId = it.id }
                    )
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                ImprovePanel(vm = vm, requestAi = requestAi, current = draft, onAccept = { draft = it })
            }
        },
        confirmButton = { TextButton(enabled = canSave, onClick = { onSave(recipientId, draft.trim()); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ImprovePanel(
    vm: StudioViewModel,
    requestAi: (() -> Unit) -> Unit,
    current: String,
    onAccept: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var tone by remember { mutableStateOf(Tone.FRIENDLY) }

    fun run(action: ImproveAction) {
        if (current.isBlank()) {
            error = "Write or paste a message first."
            return
        }
        scope.launch {
            busy = true
            error = null
            results = emptyList()
            when (val r = vm.improve(current, action, if (action == ImproveAction.CHANGE_TONE) tone else null)) {
                is AiResult.Success -> results = r.value
                is AiResult.Failure -> error = r.error.message
            }
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI tools (your message stays as it is until you tap Use this)", style = MaterialTheme.typography.labelLarge)
        Dropdown(
            label = "Tone for \"Change tone\"",
            selected = tone.label,
            options = Tone.values().toList(),
            optionLabel = { it.label },
            onSelect = { tone = it }
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImproveAction.values().forEach { action ->
                AssistChip(onClick = { requestAi { run(action) } }, label = { Text(action.label) }, enabled = !busy)
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        results.forEach { result ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(result)
                    Row {
                        TextButton(onClick = { onAccept(result); results = emptyList() }) { Text("Use this") }
                        TextButton(onClick = { results = results - result }) { Text("Discard") }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) { Text(text) }
}
