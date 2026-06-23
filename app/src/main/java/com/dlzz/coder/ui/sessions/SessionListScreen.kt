package com.dlzz.coder.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.HostSession
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel

@Composable
fun SessionListScreen(
    bridgeViewModel: BridgeViewModel,
    onSessionClick: (String, String) -> Unit
) {
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val hosts by bridgeViewModel.hosts.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)

    LaunchedEffect(hosts) {
        bridgeViewModel.refreshAllSessions()
    }

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
            SessionCard(item, onClick = { onSessionClick(item.host.id, item.session.sessionId) })
        }
    }
}

@Composable
private fun SessionCard(item: HostSession, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.session.sessionId, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(item.host.name) })
                if (item.session.providerId.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(item.session.providerId) })
                }
            }
            if (item.session.workspacePath.isNotBlank()) {
                Text(item.session.workspacePath, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
