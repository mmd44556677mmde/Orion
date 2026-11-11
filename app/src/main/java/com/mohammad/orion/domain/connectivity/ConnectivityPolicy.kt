package com.mohammad.orion.domain.connectivity

import com.mohammad.orion.domain.provider.ProviderError

/** Android's validated-network signal is not a provider reachability check. */
enum class NetworkStatus { UNKNOWN, DISCONNECTED, UNVALIDATED, VALIDATED }
enum class ProviderHealth { UNCONFIGURED, CHECKING, HEALTHY, UNAVAILABLE, AUTH_ERROR }
enum class RemoteAvailability { OFFLINE, CONNECTING, ONLINE, PROVIDER_UNAVAILABLE, CONFIGURATION_ERROR }

fun remoteAvailability(network: NetworkStatus, health: ProviderHealth): RemoteAvailability = when {
    health == ProviderHealth.UNCONFIGURED || health == ProviderHealth.AUTH_ERROR -> RemoteAvailability.CONFIGURATION_ERROR
    network == NetworkStatus.DISCONNECTED || network == NetworkStatus.UNVALIDATED -> RemoteAvailability.OFFLINE
    network == NetworkStatus.UNKNOWN -> RemoteAvailability.CONNECTING
    health == ProviderHealth.CHECKING -> RemoteAvailability.CONNECTING
    health == ProviderHealth.UNAVAILABLE -> RemoteAvailability.PROVIDER_UNAVAILABLE
    else -> RemoteAvailability.ONLINE // VALIDATED plus a successful provider-specific health check
}

/** Pure bounded full-jitter policy; it neither schedules nor authorizes a retry. */
class RetryPolicy(private val baseMillis: Long, private val maxMillis: Long, private val maxRetries: Int) {
    init {
        require(baseMillis > 0 && maxMillis >= baseMillis && maxMillis <= 86_400_000)
        require(maxRetries in 0..20)
    }

    /** retriesUsed is the number of retries already performed, not the initial attempt. */
    fun delayMillis(error: ProviderError, retriesUsed: Int, replaySafe: Boolean, partialOutput: Boolean, jitter: Double): Long? {
        require(retriesUsed >= 0)
        require(jitter.isFinite() && jitter >= 0.0 && jitter < 1.0)
        if (!replaySafe || partialOutput || retriesUsed >= maxRetries) return null
        if (error !in RETRYABLE) return null
        var ceiling = baseMillis
        repeat(retriesUsed.coerceAtMost(20)) {
            ceiling = if (ceiling > maxMillis / 2) maxMillis else (ceiling * 2).coerceAtMost(maxMillis)
        }
        return (ceiling * jitter).toLong()
    }

    companion object {
        private val RETRYABLE = setOf(ProviderError.NETWORK, ProviderError.TIMEOUT, ProviderError.RATE_LIMIT, ProviderError.SERVER)
    }
}
