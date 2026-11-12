package com.mohammad.orion.ui.tools

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohammad.orion.R
import com.mohammad.orion.domain.tools.OfflineCommandParser
import com.mohammad.orion.domain.tools.OfflineParseResult
import com.mohammad.orion.ui.design.OrionSpacing
import com.mohammad.orion.ui.design.OrionTopBar

/** Only timer/alarm proposals are supported here. The clock app retains final control. */
fun clockIntent(result: OfflineParseResult): Intent? = when (result) {
    is OfflineParseResult.Timer -> if (result.seconds in 1..86_400) Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, result.seconds).putExtra(AlarmClock.EXTRA_SKIP_UI, false) else null
    is OfflineParseResult.Alarm -> if (result.hour in 0..23 && result.minute in 0..59) Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, result.hour).putExtra(AlarmClock.EXTRA_MINUTES, result.minute)
        .putExtra(AlarmClock.EXTRA_SKIP_UI, false) else null
    else -> null
}

@Composable fun LocalClockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val parser = remember { OfflineCommandParser() }
    var input by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<OfflineParseResult?>(null) }
    var message by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { OrionTopBar(stringResource(R.string.local_clock_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Text(stringResource(R.string.local_clock_description))
            OutlinedTextField(value = input, onValueChange = { if (it.length <= 256) input = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.local_clock_command)) }, minLines = 2)
            Button(onClick = {
                val proposal = parser.parse(input)
                if (clockIntent(proposal) == null) message = R.string.local_clock_unrecognized
                else { message = 0; pending = proposal }
            }) { Text(stringResource(R.string.local_clock_review)) }
            if (message != 0) Text(stringResource(message), color = MaterialTheme.colorScheme.error)
        }
    }
    pending?.let { proposal ->
        val detail = when (proposal) {
            is OfflineParseResult.Timer -> stringResource(R.string.local_clock_timer_detail, proposal.seconds)
            is OfflineParseResult.Alarm -> stringResource(R.string.local_clock_alarm_detail, proposal.hour, proposal.minute)
            else -> ""
        }
        AlertDialog(onDismissRequest = { pending = null }, title = { Text(stringResource(R.string.local_clock_confirm_title)) }, text = { Text(detail) },
            confirmButton = { TextButton(onClick = {
                val intent = clockIntent(proposal)
                pending = null
                try { if (intent != null) context.startActivity(intent) else message = R.string.local_clock_unrecognized }
                catch (_: ActivityNotFoundException) { message = R.string.local_clock_unavailable }
                catch (_: SecurityException) { message = R.string.local_clock_unavailable }
            }) { Text(stringResource(R.string.local_clock_open)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.dismiss)) } })
    }
}
