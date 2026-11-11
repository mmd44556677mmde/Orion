package com.mohammad.orion.domain.connectivity

import com.mohammad.orion.domain.provider.ProviderError
import org.junit.Assert.*
import org.junit.Test

class ConnectivityPolicyTest {
    @Test fun networkAloneNeverProvesProviderHealth() {
        assertEquals(RemoteAvailability.CONFIGURATION_ERROR, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.UNCONFIGURED))
        assertEquals(RemoteAvailability.CONNECTING, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.CHECKING))
        assertEquals(RemoteAvailability.PROVIDER_UNAVAILABLE, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.UNAVAILABLE))
        assertEquals(RemoteAvailability.ONLINE, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.HEALTHY))
        assertEquals(RemoteAvailability.OFFLINE, remoteAvailability(NetworkStatus.UNVALIDATED, ProviderHealth.HEALTHY))
        assertEquals(RemoteAvailability.OFFLINE, remoteAvailability(NetworkStatus.DISCONNECTED, ProviderHealth.HEALTHY))
        assertEquals(RemoteAvailability.CONFIGURATION_ERROR, remoteAvailability(NetworkStatus.VALIDATED, ProviderHealth.AUTH_ERROR))
        assertEquals(RemoteAvailability.CONNECTING, remoteAvailability(NetworkStatus.UNKNOWN, ProviderHealth.HEALTHY))
    }
    @Test fun retryClassificationIsBoundedAndNeverReplaysPartialOrUnsafeRequests() {
        val policy = RetryPolicy(1000, 4000, 3)
        assertEquals(500L, policy.delayMillis(ProviderError.NETWORK, 0, true, false, 0.5))
        assertEquals(1000L, policy.delayMillis(ProviderError.TIMEOUT, 1, true, false, 0.5))
        assertEquals(2000L, policy.delayMillis(ProviderError.SERVER, 2, true, false, 0.5))
        assertNull(policy.delayMillis(ProviderError.SERVER, 3, true, false, 0.5))
        assertNull(policy.delayMillis(ProviderError.NETWORK, 0, false, false, 0.5))
        assertNull(policy.delayMillis(ProviderError.NETWORK, 0, true, true, 0.5))
        listOf(ProviderError.AUTHENTICATION, ProviderError.QUOTA, ProviderError.MALFORMED_RESPONSE, ProviderError.TLS, ProviderError.CANCELLED, ProviderError.HTTP).forEach {
            assertNull(policy.delayMillis(it, 0, true, false, 0.5))
        }
        assertNotNull(policy.delayMillis(ProviderError.RATE_LIMIT, 0, true, false, 0.0))
    }
    @Test fun cappedJitterDoesNotOverflowAndInvalidInputsFail() {
        val policy = RetryPolicy(50_000_000, 86_400_000, 20)
        assertTrue(policy.delayMillis(ProviderError.NETWORK, 19, true, false, 0.999)!! < 86_400_000)
        try { policy.delayMillis(ProviderError.NETWORK, -1, true, false, 0.5); fail() } catch (_: IllegalArgumentException) {}
        try { policy.delayMillis(ProviderError.NETWORK, 0, true, false, Double.NaN); fail() } catch (_: IllegalArgumentException) {}
        try { RetryPolicy(0, 1000, 3); fail() } catch (_: IllegalArgumentException) {}
    }
}
