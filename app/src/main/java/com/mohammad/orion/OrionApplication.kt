package com.mohammad.orion

import android.app.Application
import com.mohammad.orion.core.AppContainer
import com.mohammad.orion.core.SafeEvent

class OrionApplication : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        container.logger.event(SafeEvent.APPLICATION_STARTED)
    }
}
