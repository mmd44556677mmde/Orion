package com.mohammad.orion.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.mohammad.orion.domain.connectivity.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Observes the default network only. No request or provider health check is performed. */
class AndroidNetworkMonitor(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun observe(): Flow<NetworkStatus> = callbackFlow {
        val connectivity = manager
        if (connectivity == null) {
            trySend(NetworkStatus.UNKNOWN)
            close()
            return@callbackFlow
        }
        fun snapshot(): NetworkStatus {
            val active = connectivity.activeNetwork ?: return NetworkStatus.DISCONNECTED
            val caps = connectivity.getNetworkCapabilities(active) ?: return NetworkStatus.UNVALIDATED
            return if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) NetworkStatus.VALIDATED else NetworkStatus.UNVALIDATED
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(snapshot()) }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) { trySend(snapshot()) }
            override fun onLost(network: Network) {
                if (connectivity.activeNetwork == network) trySend(NetworkStatus.DISCONNECTED) else trySend(snapshot())
            }
        }
        var registered = false
        try {
            connectivity.registerDefaultNetworkCallback(callback)
            registered = true
            trySend(snapshot())
        } catch (_: SecurityException) {
            if (registered) connectivity.unregisterNetworkCallback(callback)
            trySend(NetworkStatus.UNKNOWN)
            close()
            return@callbackFlow
        } catch (_: RuntimeException) {
            if (registered) connectivity.unregisterNetworkCallback(callback)
            trySend(NetworkStatus.UNKNOWN)
            close()
            return@callbackFlow
        }
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
