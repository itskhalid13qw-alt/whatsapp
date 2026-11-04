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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.messagestudio.app.StudioViewModel
import com.messagestudio.app.data.MessageEntity

@Composable
fun MessagesScreen(vm: StudioViewModel, requestAi: (() -> Unit) -> Unit) {
    val recipients by vm.recipients.collectAsState()
    val selectedId by vm.selectedRecipientId.collectAsState()
    val messages by vm.messages.collectAsState()
    val context = LocalContext.current
    val selected = recipients.firstOrNull { it.id == selectedId }

    var showAddRecipient by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MessageEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var shareCandidate by remember { mutableStateOf<MessageEntity?>(null) }
    var confirmDeleteRecipient by remember { mutableStateOf(false) }

    fun share(m: MessageEntity) = shareToWhatsApp(context, selected?.phone.orEmpty(), m.text)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Messages", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Dropdown(
                        label = "Recipient",
                        selected = selected?.name ?: "No recipient yet",
                        options = recipients,
                        optionLabel = { it.name },
                        onSelect = { vm.selectRecipient(it.id) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { showAddRecipient = true }) { Text("New") }
                }
                if (selected != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Write a message") { adding = true }
                        TextButton(onClick = { confirmDeleteRecipient = true }) { Text("Delete recipient") }
                    }
                    Text("${messages.size} saved messages", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text("Add a recipient to start saving messages for them.")
                }
                SmallNote(
                    "Review every message before you share it. Sharing opens WhatsApp with the text ready, and you press " +
                        "Send yourself. Please avoid unsolicited or repetitive messaging; WhatsApp can restrict accounts that do it."
                )
            }
        }
        items(messages, key = { it.id }) { m ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(m.text)
                    SmallNote(
                        when (m.source) {
                            "AI" -> "Written by AI"
                            "LOCAL" -> "Offline template (not AI)"
                            else -> "Written by you"
                        }
                    )
                    Row {
                        TextButton(onClick = { editing = m }) { Text("Edit") }
                        TextButton(onClick = {
                            if (vm.settings.shareWarned) share(m) else shareCandidate = m
                        }) { Text("Share") }
                        TextButton(onClick = { vm.deleteMessage(m.id) }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showAddRecipient) {
        AddRecipientDialog(onDismiss = { showAddRecipient = false }, onAdd = { n, p -> vm.addRecipient(n, p) })
    }

    editing?.let { m ->
        MessageEditorDialog(
            title = "Edit message",
            initial = m.text,
            vm = vm,
            requestAi = requestAi,
            recipients = null,
            defaultRecipientId = m.recipientId,
            onDismiss = { editing = null },
            onSave = { _, text -> vm.updateMessage(m, text) }
        )
    }

    if (adding && selected != null) {
        MessageEditorDialog(
            title = "New message for ${selected.name}",
            initial = "",
            vm = vm,
            requestAi = requestAi,
            recipients = null,
            defaultRecipientId = selected.id,
            onDismiss = { adding = false },
            onSave = { _, text -> vm.saveMessage(selected.id, text, "MANUAL") }
        )
    }

    shareCandidate?.let { m ->
        AlertDialog(
            onDismissRequest = { shareCandidate = null },
            title = { Text("Before you share") },
            text = {
                Text(
                    "This opens WhatsApp with the message filled in. Nothing is sent until you press Send there. " +
                        "Only message people who expect to hear from you, and avoid sending the same text to many people."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.settings.setShareWarned(true)
                    shareCandidate = null
                    share(m)
                }) { Text("Open WhatsApp") }
            },
            dismissButton = { TextButton(onClick = { shareCandidate = null }) { Text("Cancel") } }
        )
    }

    if (confirmDeleteRecipient && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteRecipient = false },
            title = { Text("Delete ${selected.name}?") },
            text = { Text("This also deletes all ${messages.size} messages saved for this recipient.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteRecipient(selected); confirmDeleteRecipient = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteRecipient = false }) { Text("Cancel") } }
        )
    }
}
