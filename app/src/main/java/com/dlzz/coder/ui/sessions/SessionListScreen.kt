package com.dlzz.coder.ui.sessions

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
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
import com.dlzz.coder.ui.theme.glassCombinedClickable
import com.dlzz.coder.viewmodel.BridgeViewModel

@Composable
fun SessionListScreen(
    bridgeViewModel: BridgeViewModel,
    onSessionClick: (String, String) -> Unit
) {
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val hosts by bridgeViewModel.hosts.collectAsState()
    val aliases by bridgeViewModel.sessionAliases.collectAsState()
    val isRefreshing by bridgeViewModel.isRefreshing.collectAsState()
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
                Column(Modifier.weight(1f)) {
                    Text(
                        strings.sessionsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        strings.sessionsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { bridgeViewModel.refreshAllSessions() }) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = strings.refreshSessions)
                    }
                }
            }
        }
        if (hostSessions.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            strings.emptySessions,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "连接主机后创建会话",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
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
    // Meta line excludes hostName (shown in chip below) to avoid redundancy
    val meta = session.displayMeta(hostName = "")
    val time = session.relativeTime()
    val isActive = session.status.lowercase() in listOf("running", "active", "busy")

    Box(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .glassCard(cornerRadius = 16.dp, blur = if (isActive) 4.dp else 3.dp)
            .glassCombinedClickable(
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (session.status.isNotBlank() && session.status != "idle") {
                        StatusDot(status = session.status)
                        Spacer(Modifier.width(2.dp))
                    }
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                item.host.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                    if (session.providerId.isNotBlank() && session.providerId != "mock") {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    session.providerId,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
                Text(
                    "#${session.sessionId.take(8)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
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
    Box(
        Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
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
            TextButton(onClick = { onConfirm(text) }) { Text(strings.save) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text(strings.renameSessionReset) }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
}
