package com.dlzz.coder.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.debug.LogCollector
import com.dlzz.coder.debug.LogExporter
import com.dlzz.coder.ui.i18n.AppLanguage
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(bridgeViewModel: BridgeViewModel) {
    val hosts by bridgeViewModel.hosts.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val connectedCount = hosts.count { it.connected }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logRevision by remember { mutableIntStateOf(0) }
    val logCount = remember(logRevision) { LogCollector.size() }
    var showLogPreview by remember { mutableStateOf(false) }
    var selectedLogLevel by remember { mutableStateOf(LogCollector.Level.DEBUG) }
    var isExportingLogs by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)

        Box(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .glassCard(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.hostTotal(hosts.size))
                Text(strings.connectedTotal(connectedCount))
                Text(strings.protocol)
                if (connectedCount > 0) {
                    Button(onClick = { bridgeViewModel.disconnect() }) { Text(strings.disconnectAll) }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .glassCard(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.language, style = MaterialTheme.typography.titleMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AppLanguage.entries.forEach { option ->
                        FilterChip(
                            selected = language == option,
                            onClick = { bridgeViewModel.setLanguage(option) },
                            label = { Text(option.displayName) }
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .glassCard(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.diagnosticsTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.diagnosticsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        strings.logsCount(logCount),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1.45f)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        LogCollector.Level.entries.forEach { level ->
                            FilterChip(
                                selected = selectedLogLevel == level,
                                onClick = { selectedLogLevel = level },
                                label = {
                                    Text(
                                        when (level) {
                                            LogCollector.Level.DEBUG -> strings.logLevelDebug
                                            LogCollector.Level.INFO -> strings.logLevelInfo
                                            LogCollector.Level.WARN -> strings.logLevelWarn
                                            LogCollector.Level.ERROR -> strings.logLevelError
                                        },
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isExportingLogs) return@Button
                        scope.launch {
                            isExportingLogs = true
                            val logFile = LogExporter.exportLogs(context, selectedLogLevel)
                            isExportingLogs = false
                            logRevision += 1
                            if (logFile != null) {
                                val uri = LogExporter.getShareUri(context, logFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Coder App Log - ${logFile.name}")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, strings.exportLogs))
                                }.onSuccess {
                                    Toast.makeText(context, strings.exportLogsSuccess, Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    LogCollector.e("Settings", "Failed to launch log share sheet", it)
                                    Toast.makeText(context, strings.exportLogsFail, Toast.LENGTH_SHORT).show()
                                    logRevision += 1
                                }
                            } else {
                                Toast.makeText(context, strings.exportLogsFail, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isExportingLogs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExportingLogs) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isExportingLogs) strings.exportingLogs else strings.exportLogs)
                }

                Text(
                    strings.exportLogsDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showLogPreview = !showLogPreview },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (showLogPreview) strings.hideLogs else strings.previewLogs)
                }

                AnimatedVisibility(
                    visible = showLogPreview,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val logs = remember(selectedLogLevel, logRevision) {
                        LogCollector.filter(selectedLogLevel).takeLast(200)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .background(Color.Black.copy(0.05f), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    strings.noLogs,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                logs.forEach { entry ->
                                    Text(
                                        entry.formatted(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        LogCollector.clear()
                        logRevision += 1
                        Toast.makeText(context, strings.logsCleared, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.clearLogs)
                }
            }
        }
    }
}
