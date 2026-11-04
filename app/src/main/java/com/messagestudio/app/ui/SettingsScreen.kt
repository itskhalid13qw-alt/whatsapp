package com.messagestudio.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.messagestudio.app.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: StudioViewModel) {
    val s = vm.settings
    val totals by vm.usageTotals.collectAsState()
    val recent by vm.recentUsage.collectAsState()
    val dateFormat = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI provider", style = MaterialTheme.typography.titleMedium)
                SmallNote(
                    "The app talks to your own backend over HTTPS. The backend holds the AI provider's secret key, so no " +
                        "private key is stored in this app. See the backend folder for a ready-made one."
                )
                OutlinedTextField(
                    value = s.backendUrl,
                    onValueChange = { s.setBackendUrl(it) },
                    label = { Text("Backend address") },
                    placeholder = { Text("https://your-backend.example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = s.authToken,
                    onValueChange = { s.setAuthToken(it) },
                    label = { Text("Backend access token (if your backend asks for one)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SmallNote("This token is only for your backend, is kept on this phone, and is not the AI provider's key.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("I agree to send AI requests to this backend")
                        SmallNote("Turn off to be asked again before the next AI request.")
                    }
                    Switch(checked = s.aiConsent, onCheckedChange = { s.setAiConsent(it) })
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Limits", style = MaterialTheme.typography.titleMedium)
                Text("Messages per request batch: ${s.batchSize}")
                Slider(
                    value = s.batchSize.toFloat(),
                    onValueChange = { s.setBatchSize(it.toInt()) },
                    valueRange = 10f..50f,
                    steps = 7
                )
                OutlinedTextField(
                    value = s.warnAt.toString(),
                    onValueChange = { s.setWarnAt(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1) },
                    label = { Text("Warn me before requests of this many messages or more") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = s.dailyLimit.toString(),
                    onValueChange = { s.setDailyLimit(it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) },
                    label = { Text("Daily AI message limit (0 = no limit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SmallNote("AI providers may charge for usage, and their real limits depend on the service you choose. This app cannot promise unlimited generation.")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI usage", style = MaterialTheme.typography.titleMedium)
                Text("Requests: ${totals.requests} (failed: ${totals.failures})")
                Text("Messages generated today: ${vm.generatedToday}")
                if (totals.inputTokens + totals.outputTokens > 0) {
                    Text("Tokens: ${totals.inputTokens} in, ${totals.outputTokens} out")
                } else {
                    SmallNote("Token counts appear only when your backend reports them.")
                }
                recent.forEach { u ->
                    SmallNote(
                        "${dateFormat.format(Date(u.timestamp))}  ${u.kind}  " +
                            (if (u.success) "ok" else "failed ${u.error}") +
                            (if (u.messages > 0) "  ${u.messages} msgs" else "")
                    )
                }
                OutlinedButton(onClick = { vm.clearUsage() }) { Text("Clear usage history") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                SmallNote("Messages, recipients and templates are stored only on this phone.")
                SmallNote("Contacts and WhatsApp conversations are never read or sent anywhere.")
                SmallNote("Text goes to an AI service only when you use an AI button, and only after you agree.")
                SmallNote("The app never sends WhatsApp messages by itself. You review each one and press Send.")
            }
        }
    }
}
