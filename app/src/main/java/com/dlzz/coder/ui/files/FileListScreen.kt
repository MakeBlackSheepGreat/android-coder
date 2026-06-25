package com.dlzz.coder.ui.files

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.HostSession
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.ui.theme.glassClickable
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.FileViewModel

@Composable
fun FileListScreen(
    bridgeViewModel: BridgeViewModel,
    onFileClick: (String, String, String) -> Unit
) {
    val fileViewModel = remember { FileViewModel(bridgeViewModel) }
    val files by fileViewModel.files.collectAsState()
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    var selectedKey by remember { mutableStateOf("") }
    val selectedSession = remember(hostSessions, selectedKey) {
        hostSessions.firstOrNull { it.key == selectedKey } ?: hostSessions.firstOrNull()
    }

    LaunchedEffect(selectedSession?.key) {
        selectedSession?.let {
            selectedKey = it.key
            fileViewModel.listFiles(it.host.id, it.session.sessionId)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.filesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.filesSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                if (hostSessions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hostSessions.forEach { item ->
                            AssistChip(
                                onClick = { selectedKey = item.key },
                                label = {
                                    Text(
                                        item.host.name + " / " + item.session.sessionId.take(8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        if (selectedSession == null) {
            item {
                Text(strings.noSessionForFiles, style = MaterialTheme.typography.bodyMedium)
            }
        } else if (files.isEmpty()) {
            item {
                Text(strings.noFiles, style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(files, key = { it.path }) { file ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .glassCard(cornerRadius = 12.dp, blur = 2.dp, lensNear = 8.dp, lensFar = 16.dp)
                    .glassClickable {
                        selectedSession?.let { onFileClick(it.host.id, it.session.sessionId, file.path) }
                    }
                    .padding(12.dp)
            ) {
                Row {
                    Text(if (file.type == "directory") strings.directory else strings.file, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        file.path,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val HostSession.key: String
    get() = host.id + "|" + session.sessionId
