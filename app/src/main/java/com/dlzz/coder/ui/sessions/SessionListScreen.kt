package com.dlzz.coder.ui.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.HostSession
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.i18n.Strings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel

@Composable
fun SessionListScreen(
    bridgeViewModel: BridgeViewModel,
    onSessionClick: (String, String) -> Unit
) {
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val hosts by bridgeViewModel.hosts.collectAsState()
    val aliases by bridgeViewModel.sessionAliases.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)

    LaunchedEffect(hosts) {
        bridgeViewModel.refreshAllSessions()
    }

    var renamingSession by remember { mutableStateOf<HostSession?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(strings.sessionsTitle, style = MaterialTheme.typography.headlineMedium)
                    Text(strings.sessionsSubtitle, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { bridgeViewModel.refreshAllSessions() }) {
                    Icon(Icons.Default.Refresh, contentDescription = strings.refreshSessions)
                }
            }
        }
        if (hostSessions.isEmpty()) {
            item {
                Text(strings.emptySessions, style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(hostSessions, key = { it.host.id + ":" + it.session.sessionId }) { item ->
            SessionCard(
                item = item,
                alias = aliases[item.session.sessionId].orEmpty(),
                strings = strings,
                onClick = { onSessionClick(item.host.id, item.session.sessionId) },
                onLongClick = { renamingSession = item }
            )
        }
    }

    renamingSession?.let { session ->
        RenameSessionDialog(
            currentName = session.session.displayName(aliases[session.session.sessionId].orEmpty()),
            onDismiss = { renamingSession = null },
            onConfirm = { newAlias ->
                bridgeViewModel.renameSession(session.session.sessionId, newAlias)
                renamingSession = null
            },
            onClear = {
                bridgeViewModel.renameSession(session.session.sessionId, "")
                renamingSession = null
            },
            strings = strings
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    item: HostSession,
    alias: String,
    strings: Strings,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val session = item.session
    val title = session.displayName(alias)
    val meta = session.displayMeta(item.host.name)
    val time = session.relativeTime()

    Box(
        Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (time.isNotEmpty()) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.status.isNotBlank() && session.status != "idle") {
                    StatusDot(status = session.status)
                    Spacer(Modifier.width(2.dp))
                }
                AssistChip(onClick = {}, label = { Text(item.host.name, maxLines = 1) })
                if (session.providerId.isNotBlank() && session.providerId != "mock") {
                    AssistChip(onClick = {}, label = { Text(session.providerId, maxLines = 1) })
                }
            }
            Text(
                session.sessionId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = when (status.lowercase()) {
        "running", "active", "busy" -> Color(0xFF34C759)
        "waiting", "paused" -> Color(0xFFFF9500)
        "error", "failed" -> Color(0xFFFF3B30)
        else -> Color.Gray
    }
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun RenameSessionDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
    strings: Strings
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renameSessionTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(strings.renameSessionLabel) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(strings.create) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text(strings.renameSessionReset) }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}
