package com.messagestudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.messagestudio.app.StudioViewModel

private data class TabItem(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("Messages", Icons.Default.Email),
    TabItem("AI Studio", Icons.Default.Star),
    TabItem("Templates", Icons.Default.List),
    TabItem("Assistant", Icons.Default.Face),
    TabItem("Settings", Icons.Default.Settings)
)

@Composable
fun StudioApp(vm: StudioViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    var pendingAi by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Every AI action goes through here: if the user has not agreed yet, ask first.
    val requestAi: (() -> Unit) -> Unit = { action ->
        if (vm.settings.aiConsent) action() else pendingAi = action
    }

    LaunchedEffect(vm.notice) {
        vm.notice?.let {
            snackbar.showSnackbar(it)
            vm.notice = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> MessagesScreen(vm, requestAi)
                1 -> AiStudioScreen(vm, requestAi, goMessages = { tab = 0 })
                2 -> TemplatesScreen(vm, requestAi, useTemplate = { vm.pendingTemplate = it; tab = 1 })
                3 -> AssistantScreen(vm, requestAi)
                else -> SettingsScreen(vm)
            }
        }
    }

    pendingAi?.let { action ->
        ConsentDialog(
            backendUrl = vm.settings.backendUrl,
            onAllow = {
                vm.settings.setAiConsent(true)
                pendingAi = null
                action()
            },
            onCancel = { pendingAi = null }
        )
    }
}
