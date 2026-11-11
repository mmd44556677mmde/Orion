package com.mohammad.orion.ui.design

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohammad.orion.R

object OrionShapes { val card = RoundedCornerShape(16.dp); val control = RoundedCornerShape(10.dp) }
enum class OrionStatus(@StringRes val label: Int) {
    SUCCESS(R.string.status_success), WARNING(R.string.status_warning), ERROR(R.string.status_error), OFFLINE(R.string.status_offline), CONNECTING(R.string.status_connecting)
}
@Composable fun statusColor(status: OrionStatus): Color = when (status) {
    OrionStatus.SUCCESS -> MaterialTheme.colorScheme.primary
    OrionStatus.WARNING -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) OrionColors.Warning else Color(0xFF765900)
    OrionStatus.ERROR -> MaterialTheme.colorScheme.error
    OrionStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    OrionStatus.CONNECTING -> MaterialTheme.colorScheme.secondary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun OrionTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
        if (onBack != null) OrionIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
    }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
}
@Composable fun OrionStatusIndicator(status: OrionStatus, modifier: Modifier = Modifier) {
    val label = stringResource(status.label)
    val description = stringResource(R.string.status_description, label)
    val color = statusColor(status)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = description }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)) {
        androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
@Composable fun OrionAssistantState(status: OrionStatus, modifier: Modifier = Modifier) {
    val label = stringResource(status.label)
    val description = stringResource(R.string.assistant_state, label)
    Row(modifier.clearAndSetSemantics { contentDescription = description }) {
        OrionStatusIndicator(status)
    }
}
@Composable fun OrionTerminalText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
}
@Composable fun OrionPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.defaultMinSize(minHeight = 48.dp), shape = OrionShapes.control) { Text(text) }
}
@Composable fun OrionSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.defaultMinSize(minHeight = 48.dp), shape = OrionShapes.control) { Text(text) }
}
@Composable fun OrionIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(icon, contentDescription = description) }
}
@Composable fun OrionTextInput(value: String, onValueChange: (String) -> Unit, label: String, hint: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onValueChange, modifier = modifier, label = { Text(label) }, placeholder = { Text(hint) }, shape = OrionShapes.control)
}
@Composable fun OrionDialog(title: String, body: String, confirm: String, dismiss: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) }, confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } })
}
@Composable fun OrionSnackbar(data: SnackbarData, modifier: Modifier = Modifier) { Snackbar(modifier = modifier, snackbarData = data) }
@Composable fun OrionLoadingState(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Box(modifier.fillMaxWidth().padding(OrionSpacing.lg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}
@Composable fun OrionEmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun OrionErrorState(title: String, detail: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.sm)) {
        Text(title, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
        OrionSecondaryButton(stringResource(R.string.retry), onRetry)
    }
}
@Composable fun OrionProgressCard(title: String, detail: String, progress: Float, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = OrionShapes.card, elevation = CardDefaults.cardElevation(defaultElevation = OrionElevation.card)) {
        Column(Modifier.padding(OrionSpacing.md), verticalArrangement = Arrangement.spacedBy(OrionSpacing.sm)) {
            Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth())
        }
    }
}
@Composable fun OrionSettingRow(title: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = OrionShapes.control, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.defaultMinSize(minHeight = 48.dp).padding(OrionSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium); Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
