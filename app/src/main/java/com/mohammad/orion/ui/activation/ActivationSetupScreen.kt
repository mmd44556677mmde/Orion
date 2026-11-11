package com.mohammad.orion.ui.activation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mohammad.orion.R
import com.mohammad.orion.ui.design.OrionPrimaryButton
import com.mohammad.orion.ui.design.OrionSecondaryButton
import com.mohammad.orion.ui.design.OrionSpacing
import com.mohammad.orion.ui.design.OrionTopBar

/** Guidance only: this screen does not request microphone access or start a service. */
@Composable
fun ActivationSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var settingsUnavailable by remember { mutableStateOf(false) }
    fun open(intent: Intent) {
        settingsUnavailable = false
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            settingsUnavailable = true
        } catch (_: SecurityException) {
            settingsUnavailable = true
        }
    }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.activation_title), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OrionSpacing.md),
        ) {
            Text(stringResource(R.string.activation_unavailable), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.activation_current_limit), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.activation_model_note), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.activation_privacy), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.activation_hyperos_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.activation_hyperos_autostart), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.activation_hyperos_battery), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.activation_hyperos_recents), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.activation_system_restrictions), style = MaterialTheme.typography.bodyMedium)
            OrionPrimaryButton(stringResource(R.string.activation_app_settings), onClick = {
                open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            })
            OrionSecondaryButton(stringResource(R.string.activation_battery_settings), onClick = {
                open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
            if (settingsUnavailable) Text(stringResource(R.string.activation_settings_unavailable), color = MaterialTheme.colorScheme.error)
        }
    }
}
