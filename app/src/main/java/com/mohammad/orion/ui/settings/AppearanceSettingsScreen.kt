package com.mohammad.orion.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.mohammad.orion.data.settings.AppearanceRepository
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.domain.settings.ThemeMode
import com.mohammad.orion.ui.design.OrionSpacing
import com.mohammad.orion.ui.design.OrionTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable fun AppearanceSettingsScreen(onBack: () -> Unit, state: StateFlow<AppearanceLoad>, repository: AppearanceRepository) {
    val load by state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saveFailed by remember { mutableStateOf(false) }
    fun save(block: suspend () -> Unit) {
        scope.launch {
            saveFailed = false
            try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { saveFailed = true }
        }
    }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.appearance_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Text(stringResource(R.string.appearance_description), style = MaterialTheme.typography.bodyMedium)
            when (val current = load) {
                AppearanceLoad.Loading -> Text(stringResource(R.string.loading))
                AppearanceLoad.Unavailable -> Text(stringResource(R.string.appearance_read_failed), color = MaterialTheme.colorScheme.error)
                is AppearanceLoad.Ready -> {
                    Column(Modifier.selectableGroup()) {
                        ThemeMode.entries.forEach { mode ->
                            val name = when (mode) {
                                ThemeMode.DARK -> R.string.appearance_dark
                                ThemeMode.LIGHT -> R.string.appearance_light
                                ThemeMode.SYSTEM -> R.string.appearance_system
                            }
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = current.settings.theme == mode, onClick = { save { repository.setTheme(mode) } }, role = Role.RadioButton), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = current.settings.theme == mode, onClick = null)
                                Text(stringResource(name), modifier = Modifier.padding(start = OrionSpacing.sm))
                            }
                        }
                    }
                    val supported = Build.VERSION.SDK_INT >= 31
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = current.settings.dynamicColor, enabled = supported, role = Role.Switch, onValueChange = { enabled -> save { repository.setDynamicColor(enabled) } }), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.appearance_dynamic), modifier = Modifier.weight(1f))
                        Switch(checked = current.settings.dynamicColor, onCheckedChange = null, enabled = supported)
                    }
                    Text(stringResource(if (supported) R.string.appearance_dynamic_hint else R.string.appearance_dynamic_unsupported), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (saveFailed) Text(stringResource(R.string.appearance_save_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
