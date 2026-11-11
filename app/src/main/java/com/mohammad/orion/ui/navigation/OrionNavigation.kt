package com.mohammad.orion.ui.navigation

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import com.mohammad.orion.domain.connectivity.NetworkStatus
import com.mohammad.orion.assistant.AssistantSetupScreen
import com.mohammad.orion.ui.activation.ActivationSetupScreen
import com.mohammad.orion.ui.settings.AppearanceSettingsScreen
import com.mohammad.orion.ui.settings.SecuritySettingsScreen
import com.mohammad.orion.data.settings.SecureScreenState
import com.mohammad.orion.data.settings.SecureScreenRepository
import com.mohammad.orion.data.settings.AppearanceRepository
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.BuildConfig
import com.mohammad.orion.R
import com.mohammad.orion.ui.design.*

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val ASSISTANT_SETUP = "assistant_setup"
private const val ACTIVATION_SETUP = "activation_setup"
private const val APPEARANCE = "appearance"
private const val SECURITY = "security"

@Composable fun OrionNavigation(networkStatus: StateFlow<NetworkStatus>, appearance: StateFlow<AppearanceLoad>, appearanceRepository: AppearanceRepository, security: StateFlow<SecureScreenState>, securityRepository: SecureScreenRepository, onEnableScreenProtection: () -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = HOME) {
        composable(HOME) {
            val network by networkStatus.collectAsStateWithLifecycle()
            HomeScreen(network = network, onSettings = { nav.navigate(SETTINGS) { launchSingleTop = true } })
        }
        composable(SETTINGS) { SettingsScreen(
            onBack = { nav.popBackStack() },
            onAssistantSetup = { nav.navigate(ASSISTANT_SETUP) { launchSingleTop = true } },
            onActivationSetup = { nav.navigate(ACTIVATION_SETUP) { launchSingleTop = true } },
            onAppearance = { nav.navigate(APPEARANCE) { launchSingleTop = true } },
            onSecurity = { nav.navigate(SECURITY) { launchSingleTop = true } },
        ) }
        composable(ASSISTANT_SETUP) { AssistantSetupScreen(onBack = { nav.popBackStack() }) }
        composable(ACTIVATION_SETUP) { ActivationSetupScreen(onBack = { nav.popBackStack() }) }
        composable(APPEARANCE) { AppearanceSettingsScreen(onBack = { nav.popBackStack() }, state = appearance, repository = appearanceRepository) }
        composable(SECURITY) { SecuritySettingsScreen(onBack = { nav.popBackStack() }, state = security, repository = securityRepository, onEnableProtection = onEnableScreenProtection) }
    }
}

@Composable private fun HomeScreen(network: NetworkStatus, onSettings: () -> Unit) {
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.app_name), actions = {
        OrionIconButton(Icons.Default.Settings, stringResource(R.string.settings), onSettings)
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            OrionTerminalText(stringResource(R.string.by_creator))
            Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.home_description), style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            OrionTerminalText(stringResource(R.string.system_status))
            OrionAssistantState(OrionStatus.OFFLINE)
            NetworkSummary(network)
            OrionProgressCard(stringResource(R.string.foundation_ready), stringResource(R.string.foundation_detail), 1f)
            OrionSecondaryButton(stringResource(R.string.settings), onSettings)
            OrionEmptyState(stringResource(R.string.no_connections), stringResource(R.string.no_connections_detail))
            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                Text(stringResource(R.string.debug_information), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.debug_details, Build.VERSION.SDK_INT, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(OrionSpacing.lg))
        }
    }
}
@Composable private fun SettingsScreen(onBack: () -> Unit, onAssistantSetup: () -> Unit, onActivationSetup: () -> Unit, onAppearance: () -> Unit, onSecurity: () -> Unit) {
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.settings), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md)) {
            OrionEmptyState(stringResource(R.string.settings), stringResource(R.string.settings_description))
            OrionSettingRow(stringResource(R.string.assistant_setup_title), stringResource(R.string.assistant_setup_short), onAssistantSetup)
            OrionSettingRow(stringResource(R.string.activation_title), stringResource(R.string.activation_setup_short), onActivationSetup)
            OrionSettingRow(stringResource(R.string.appearance_title), stringResource(R.string.appearance_short), onAppearance)
            OrionSettingRow(stringResource(R.string.security_title), stringResource(R.string.security_short), onSecurity)
        }
    }
}
