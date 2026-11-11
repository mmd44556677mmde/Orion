package com.mohammad.orion.data.settings

sealed interface SecureScreenState {
    data object Loading : SecureScreenState
    data object Unavailable : SecureScreenState
    data class Ready(val enabled: Boolean) : SecureScreenState
}


fun secureScreenRequired(state: SecureScreenState): Boolean = (state as? SecureScreenState.Ready)?.enabled ?: true
