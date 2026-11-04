package com.messagestudio.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messagestudio.app.StudioViewModel

private val SUGGESTIONS = listOf(
    "Help me write a polite reminder in Urdu",
    "What generation options do I have?",
    "Suggest a style for birthday wishes to a colleague",
    "Help me write a reusable prompt for friendly Roman Urdu greetings"
)

@Composable
fun AssistantScreen(vm: StudioViewModel, requestAi: (() -> Unit) -> Unit) {
    val recipients by vm.recipients.collectAsState()
    val selectedId by vm.selectedRecipientId.collectAsState()
    var input by remember { mutableStateOf("") }
    var draftToEdit by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(vm.chat.size, vm.chatBusy) {
        if (vm.chat.isNotEmpty()) listState.animateScrollToItem(vm.chat.size)
    }

    fun send(text: String) {
        if (text.isBlank()) return
        requestAi {
            vm.sendChat(text)
            input = ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Assistant", style = MaterialTheme.typography.headlineSmall)
                    SmallNote(
                        "Ask for help writing messages. The assistant can ask you questions, suggest styles and draft messages. " +
                            "It never sends anything on WhatsApp. Your chat here is sent to your AI backend."
                    )
                    if (vm.chat.isEmpty()) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SUGGESTIONS.forEach { s -> AssistChip(onClick = { send(s) }, label = { Text(s) }) }
                        }
                    } else {
                        TextButton(onClick = { vm.clearChat() }) { Text("Clear chat") }
                    }
                }
            }
            items(vm.chat.toList()) { turn ->
                val mine = turn.role == "user"
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = if (mine) 40.dp else 0.dp, end = if (mine) 0.dp else 40.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (turn.text.isNotBlank()) Text(turn.text)
                        turn.drafts.forEach { draft ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(draft)
                                    TextButton(onClick = { draftToEdit = draft }) { Text("Open in editor") }
                                }
                            }
                        }
                    }
                }
            }
            if (vm.chatBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            vm.chatError?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error) } }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask the assistant...") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            PrimaryButton("Send", enabled = input.isNotBlank() && !vm.chatBusy) { send(input) }
        }
    }

    draftToEdit?.let { draft ->
        MessageEditorDialog(
            title = "Save this message",
            initial = draft,
            vm = vm,
            requestAi = requestAi,
            recipients = recipients,
            defaultRecipientId = selectedId,
            onDismiss = { draftToEdit = null },
            onSave = { rid, text -> if (rid != null) vm.saveMessage(rid, text, "AI") }
        )
    }
}
