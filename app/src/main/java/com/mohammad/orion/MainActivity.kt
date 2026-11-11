package com.mohammad.orion

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mohammad.orion.data.settings.SecureScreenState
import com.mohammad.orion.data.settings.secureScreenRequired
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohammad.orion.domain.settings.AppearanceLoad
import com.mohammad.orion.domain.settings.AppearanceSettings
import com.mohammad.orion.domain.settings.useDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mohammad.orion.ui.design.OrionTheme
import com.mohammad.orion.ui.navigation.OrionNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val container = (application as OrionApplication).container
        setContent {
            val appearance by container.appearance.collectAsStateWithLifecycle()
            val settings = (appearance as? AppearanceLoad.Ready)?.settings ?: AppearanceSettings()
            val security by container.secureScreen.collectAsStateWithLifecycle()
            var requireSecureNow by remember { mutableStateOf(false) }
            // Read state during composition so changes schedule the window update.
            val protectScreen = requireSecureNow || secureScreenRequired(security)
            val protectionPersisted = (security as? SecureScreenState.Ready)?.enabled == true
            SideEffect {
                if (protectScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (requireSecureNow && protectionPersisted) requireSecureNow = false
            }
            OrionTheme(dark = useDarkTheme(settings.theme, isSystemInDarkTheme()), dynamic = settings.dynamicColor) {
                OrionNavigation(container.networkStatus, container.appearance, container.appearanceRepository, container.secureScreen, container.secureScreenRepository, onEnableScreenProtection = {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    requireSecureNow = true
                })
            }
        }
    }
}
