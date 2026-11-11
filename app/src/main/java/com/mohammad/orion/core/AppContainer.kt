package com.mohammad.orion.core

import android.util.Log
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.mohammad.orion.data.settings.AssistantNameRepository
import com.mohammad.orion.domain.settings.AssistantNameState
import com.mohammad.orion.data.settings.AppearanceRepository
import com.mohammad.orion.data.settings.SecureScreenRepository
import com.mohammad.orion.data.settings.SecureScreenState
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.data.connectivity.AndroidNetworkMonitor
import com.mohammad.orion.data.visual.AssistContextReader
import com.mohammad.orion.domain.connectivity.NetworkStatus
import com.mohammad.orion.domain.live.LiveStateMachine
import com.mohammad.orion.domain.memory.MemoryPolicyEvaluator
import com.mohammad.orion.domain.memory.MemorySecretScanner
import com.mohammad.orion.domain.memory.MemoryRetrieval
import com.mohammad.orion.domain.memory.ExplicitMemoryCommandParser
import com.mohammad.orion.domain.notifications.NotificationPrivacyPolicy
import com.mohammad.orion.domain.routines.RoutineScheduleCalculator
import com.mohammad.orion.domain.orchestration.RequestRouter
import com.mohammad.orion.domain.security.ConfirmationGate
import com.mohammad.orion.domain.tools.OfflineCommandParser
import com.mohammad.orion.domain.voice.SpokenTextSegmenter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Explicit composition root. No Android permission or service is exposed to a model. */
class AppContainer(context: Context) {
    val dispatchers = AppDispatchers()
    val applicationScope = CoroutineScope(SupervisorJob() + dispatchers.default)
    val logger: SafeLogger = AndroidSafeLogger()
    private val settingsStore = PreferenceDataStoreFactory.create(scope = applicationScope) { context.applicationContext.preferencesDataStoreFile("appearance") }
    val assistantNameRepository = AssistantNameRepository(settingsStore)
    val assistantName = assistantNameRepository.state.stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), AssistantNameState.Loading)
    val appearanceRepository = AppearanceRepository(settingsStore)
    val appearance = appearanceRepository.state.stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), AppearanceLoad.Loading)
    val secureScreenRepository = SecureScreenRepository(settingsStore)
    val secureScreen = secureScreenRepository.state.stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), SecureScreenState.Loading)
    val memoryPolicyEvaluator = MemoryPolicyEvaluator()
    val memorySecretScanner = MemorySecretScanner()
    val memoryRetrieval = MemoryRetrieval()
    val explicitMemoryCommandParser = ExplicitMemoryCommandParser(memorySecretScanner)
    val requestRouter = RequestRouter()
    val confirmationGate = ConfirmationGate()
    val offlineCommandParser = OfflineCommandParser()
    val notificationPrivacyPolicy = NotificationPrivacyPolicy(memorySecretScanner)
    val routineScheduleCalculator = RoutineScheduleCalculator()
    val spokenTextSegmenter = SpokenTextSegmenter()
    val liveStateMachine = LiveStateMachine()
    val assistContextReader = AssistContextReader(dispatcher = dispatchers.default)
    val networkStatus = AndroidNetworkMonitor(context).observe().stateIn(applicationScope, SharingStarted.WhileSubscribed(5_000), NetworkStatus.UNKNOWN)
}

class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
)

sealed interface OrionError {
    data object Unavailable : OrionError
    data object InvalidInput : OrionError
    data object Unauthorized : OrionError
    data object Unexpected : OrionError
}

/** Only fixed, reviewed events may be logged; arbitrary user data and exceptions are not accepted. */
enum class SafeEvent { APPLICATION_STARTED }
interface SafeLogger { fun event(event: SafeEvent) }
private class AndroidSafeLogger : SafeLogger {
    override fun event(event: SafeEvent) {
        if (com.mohammad.orion.BuildConfig.DEBUG) Log.d("Orion", event.name)
    }
}
