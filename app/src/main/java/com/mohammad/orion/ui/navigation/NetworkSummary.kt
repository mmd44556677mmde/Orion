package com.mohammad.orion.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mohammad.orion.R
import com.mohammad.orion.domain.connectivity.NetworkStatus
import com.mohammad.orion.ui.design.OrionSpacing

/** A transport signal is shown separately from the unconfigured remote provider. */
@Composable fun NetworkSummary(network: NetworkStatus) {
    val label = when (network) {
        NetworkStatus.UNKNOWN -> R.string.network_unknown
        NetworkStatus.DISCONNECTED -> R.string.network_disconnected
        NetworkStatus.UNVALIDATED -> R.string.network_unvalidated
        NetworkStatus.VALIDATED -> R.string.network_validated
    }
    Column(verticalArrangement = Arrangement.spacedBy(OrionSpacing.xs)) {
        Text(stringResource(R.string.local_interface_available), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.remote_provider_unconfigured), style = MaterialTheme.typography.bodyMedium)
    }
}
