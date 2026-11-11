package com.mohammad.orion.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammad.orion.R
import com.mohammad.orion.data.settings.SecureScreenRepository
import com.mohammad.orion.data.settings.SecureScreenState
import com.mohammad.orion.ui.design.OrionSpacing
import com.mohammad.orion.ui.design.OrionTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable fun SecuritySettingsScreen(onBack: () -> Unit, state: StateFlow<SecureScreenState>, repository: SecureScreenRepository, onEnableProtection: () -> Unit) {
    val current by state.collectAsStateWithLifecycle()
    var confirmDisable by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun save(enabled: Boolean) {
        scope.launch {
            saveFailed = false
            try { repository.setEnabled(enabled) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { saveFailed = true }
        }
    }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.security_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Text(stringResource(R.string.secure_screen_description), style = MaterialTheme.typography.bodyMedium)
            when (val value = current) {
                SecureScreenState.Loading -> Text(stringResource(R.string.loading))
                SecureScreenState.Unavailable -> Text(stringResource(R.string.secure_screen_read_failed), color = MaterialTheme.colorScheme.error)
                is SecureScreenState.Ready -> {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = value.enabled, role = Role.Switch, onValueChange = { if (it) { onEnableProtection(); save(true) } else confirmDisable = true }), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.secure_screen_label), modifier = Modifier.weight(1f))
                        Switch(checked = value.enabled, onCheckedChange = null)
                    }
                    Text(stringResource(R.string.secure_screen_limit), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (saveFailed) Text(stringResource(R.string.secure_screen_save_failed), color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmDisable) AlertDialog(
        onDismissRequest = { confirmDisable = false },
        title = { Text(stringResource(R.string.secure_screen_disable_title)) },
        text = { Text(stringResource(R.string.secure_screen_disable_message)) },
        confirmButton = { TextButton(onClick = { confirmDisable = false; if ((current as? SecureScreenState.Ready)?.enabled == true) save(false) }) { Text(stringResource(R.string.secure_screen_disable_confirm)) } },
        dismissButton = { TextButton(onClick = { confirmDisable = false }) { Text(stringResource(R.string.dismiss)) } },
    )
}
