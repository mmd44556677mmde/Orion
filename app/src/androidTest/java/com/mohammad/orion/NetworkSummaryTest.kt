package com.mohammad.orion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohammad.orion.domain.connectivity.NetworkStatus
import com.mohammad.orion.ui.design.OrionTheme
import com.mohammad.orion.ui.navigation.NetworkSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkSummaryTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun networkChangesDoNotClaimAProviderIsConnected() {
        var network by mutableStateOf(NetworkStatus.UNKNOWN)
        rule.setContent { OrionTheme { NetworkSummary(network) } }
        rule.onNodeWithText(rule.activity.getString(R.string.network_unknown)).assertIsDisplayed()
        for ((state, id) in listOf(NetworkStatus.DISCONNECTED to R.string.network_disconnected, NetworkStatus.UNVALIDATED to R.string.network_unvalidated, NetworkStatus.VALIDATED to R.string.network_validated)) {
            rule.runOnIdle { network = state }
            rule.onNodeWithText(rule.activity.getString(id)).assertIsDisplayed()
            rule.onNodeWithText(rule.activity.getString(R.string.remote_provider_unconfigured)).assertIsDisplayed()
        }
    }
}
