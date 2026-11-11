package com.mohammad.orion.assistant

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohammad.orion.R
import com.mohammad.orion.ui.design.*

private fun selection(context: Context): AssistantSelection {
    val component = ComponentName(context, OrionVoiceService::class.java)
    val selected = VoiceInteractionService.isActiveService(context, component)
    val available = if (Build.VERSION.SDK_INT >= 29) {
        context.getSystemService(RoleManager::class.java)?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
    } else true
    return assistantSelection(available, selected)
}

@Composable fun AssistantSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var current by remember { mutableStateOf(selection(context)) }
    var launchFailed by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        current = selection(context)
    }
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) current = selection(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.assistant_setup_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Text(stringResource(R.string.assistant_setup_description), style = MaterialTheme.typography.bodyLarge)
            val status = when (current) {
                AssistantSelection.SELECTED -> R.string.assistant_selected
                AssistantSelection.NOT_SELECTED -> R.string.assistant_not_selected
                AssistantSelection.UNAVAILABLE -> R.string.assistant_unavailable
            }
            Text(stringResource(status), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.assistant_setup_limitations), style = MaterialTheme.typography.bodyMedium)
            OrionPrimaryButton(stringResource(R.string.assistant_open_settings), onClick = {
                launchFailed = false
                try {
                    val roleManager = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
                    val intent = if (Build.VERSION.SDK_INT >= 29 && roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                    } else Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    launcher.launch(intent)
                } catch (_: ActivityNotFoundException) { launchFailed = true
                } catch (_: SecurityException) { launchFailed = true }
            })
            if (launchFailed) Text(stringResource(R.string.assistant_settings_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
