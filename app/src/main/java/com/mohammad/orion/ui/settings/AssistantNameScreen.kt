package com.mohammad.orion.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammad.orion.R
import com.mohammad.orion.data.settings.AssistantNameRepository
import com.mohammad.orion.domain.settings.AssistantNameState
import com.mohammad.orion.domain.settings.normalizeAssistantName
import com.mohammad.orion.ui.design.OrionSpacing
import com.mohammad.orion.ui.design.OrionTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable fun AssistantNameScreen(onBack: () -> Unit, state: StateFlow<AssistantNameState>, repository: AssistantNameRepository) {
    val current by state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var draft by remember(current) { mutableStateOf((current as? AssistantNameState.Ready)?.name.orEmpty()) }
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    fun save(reset: Boolean) {
        if (saving) return
        scope.launch {
            saving = true
            failed = false
            try { if (reset) repository.reset() else repository.setName(draft) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { saving = false }
        }
    }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.assistant_name_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Text(stringResource(R.string.assistant_name_description), style = MaterialTheme.typography.bodyMedium)
            when (current) {
                AssistantNameState.Loading -> Text(stringResource(R.string.loading))
                AssistantNameState.Unavailable -> Text(stringResource(R.string.assistant_name_read_failed), color = MaterialTheme.colorScheme.error)
                is AssistantNameState.Ready -> {
                    OutlinedTextField(value = draft, onValueChange = { if (it.length <= 40) draft = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.assistant_name_label)) }, supportingText = { Text(stringResource(R.string.assistant_name_hint)) })
                    Button(onClick = { save(false) }, enabled = !saving && normalizeAssistantName(draft) != null) { Text(stringResource(R.string.assistant_name_save)) }
                    OutlinedButton(onClick = { save(true) }, enabled = !saving) { Text(stringResource(R.string.assistant_name_reset)) }
                }
            }
            if (failed) Text(stringResource(R.string.assistant_name_save_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}
