package com.dlzz.coder.ui.hosts

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.BridgeHost
import com.dlzz.coder.scan.PortraitCaptureActivity
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.i18n.Strings
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.google.zxing.integration.android.IntentIntegrator
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun HostListScreen(
    bridgeViewModel: BridgeViewModel
) {
    val hosts by bridgeViewModel.hosts.collectAsState()
    val sessionsByHost by bridgeViewModel.sessionsByHost.collectAsState()
    val scanState by bridgeViewModel.scanState.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var sessionHost by remember { mutableStateOf<BridgeHost?>(null) }
    var errorText by remember { mutableStateOf("") }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scanResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        val contents = scanResult?.contents.orEmpty()
        if (contents.isNotBlank()) {
            bridgeViewModel.addHostFromQr(contents)
                .onFailure { errorText = it.message ?: strings.unsupportedQr }
        }
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
                    Text(strings.hostsTitle, style = MaterialTheme.typography.headlineMedium)
                    Text(strings.hostsSubtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.add)
                }
                OutlinedButton(
                    onClick = {
                        val activity = context as? Activity ?: return@OutlinedButton
                        val intent = IntentIntegrator(activity)
                            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                            .setCaptureActivity(PortraitCaptureActivity::class.java)
                            .setOrientationLocked(true)
                            .setPrompt(strings.scanPrompt)
                            .setBeepEnabled(false)
                            .createScanIntent()
                        scannerLauncher.launch(intent)
                    }
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.scan)
                }
                OutlinedButton(onClick = { showScanDialog = true }) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(strings.scanLan)
                }
            }
        }
        if (scanState.message.isNotBlank()) {
            item {
                Text(scanState.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (errorText.isNotBlank()) {
            item {
                Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (hosts.isEmpty()) {
            item {
                EmptyHostState(strings)
            }
        }
        items(hosts, key = { it.id }) { host ->
            HostCard(
                host = host,
                sessionCount = sessionsByHost[host.id].orEmpty().size,
                strings = strings,
                onSelect = { bridgeViewModel.selectHost(host.id) },
                onConnect = { bridgeViewModel.connectHost(host.id) },
                onDisconnect = { bridgeViewModel.disconnectHost(host.id) },
                onDelete = { bridgeViewModel.removeHost(host.id) },
                onNewSession = { sessionHost = host }
            )
        }
    }

    if (showAddDialog) {
        AddHostDialog(
            strings = strings,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, host, port, token, providerId, workspacePath, workspaceTitle ->
                bridgeViewModel.addHost(
                    name = name,
                    host = host,
                    port = port,
                    token = token,
                    providerId = providerId,
                    workspacePath = workspacePath,
                    workspaceTitle = workspaceTitle
                )
                showAddDialog = false
            }
        )
    }

    if (showScanDialog) {
        ScanLanDialog(
            strings = strings,
            scanState = scanState,
            onDismiss = { showScanDialog = false },
            onStart = { targets, port, token ->
                bridgeViewModel.scanAndAddHosts(
                    targetText = targets,
                    port = port,
                    token = token
                )
            }
        )
    }

    sessionHost?.let { host ->
        NewSessionDialog(
            host = host,
            strings = strings,
            onDismiss = { sessionHost = null },
            onConfirm = { providerId, workspacePath, workspaceTitle ->
                bridgeViewModel.createSession(host.id, providerId, workspacePath, workspaceTitle)
                sessionHost = null
            }
        )
    }
}

@Composable
private fun ScanLanDialog(
    strings: Strings,
    scanState: BridgeViewModel.ScanState,
    onDismiss: () -> Unit,
    onStart: (String, Int, String) -> Unit
) {
    var targets by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var token by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.scanLanTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targets,
                    onValueChange = { targets = it },
                    label = { Text(strings.scanTargets) },
                    placeholder = { Text(strings.scanTargetsHint) },
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text(strings.port) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(strings.token) },
                    placeholder = { Text(strings.scanTokenHint) },
                    singleLine = true
                )
                Text(
                    text = when {
                        scanState.running -> strings.scanRunning
                        scanState.message.isNotBlank() -> scanState.message
                        else -> strings.scanIdle
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (scanState.total > 0) {
                    Text(
                        text = "${scanState.scanned}/${scanState.total}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !scanState.running,
                onClick = { onStart(targets, port.toIntOrNull() ?: 8787, token) }
            ) {
                Text(strings.startScan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun EmptyHostState(strings: Strings) {
    val isDark = isSystemInDarkTheme()
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = rememberCanvasBackdrop {},
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(if (isDark) Color.White.copy(0.06f) else Color.White.copy(0.5f))
                }
            )
            .padding(16.dp)
    ) {
        Text(strings.emptyHosts)
    }
}

@Composable
private fun HostCard(
    host: BridgeHost,
    sessionCount: Int,
    strings: Strings,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onNewSession: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) Color.White.copy(0.08f) else Color.White.copy(0.6f)
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = rememberCanvasBackdrop {},
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clickable(onClick = onSelect)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(host.name, style = MaterialTheme.typography.titleMedium)
                    Text(host.address, style = MaterialTheme.typography.bodyMedium)
                    if (host.workspacePath.isNotBlank()) {
                        Text(host.workspacePath, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = strings.deleteHost)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (host.connected) strings.connected else strings.disconnected) }
                )
                AssistChip(onClick = {}, label = { Text(strings.sessionCount(sessionCount)) })
                if (host.providerId.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text(host.providerId) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (host.connected) {
                    OutlinedButton(onClick = onDisconnect) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.disconnect)
                    }
                    Button(onClick = onNewSession) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.newSession)
                    }
                } else {
                    Button(onClick = onConnect) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.connect)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHostDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var token by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf("") }
    var workspacePath by remember { mutableStateOf("") }
    var workspaceTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addHost) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.name) }, singleLine = true)
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(strings.hostAddress) }, singleLine = true)
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text(strings.port) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text(strings.token) }, singleLine = true)
                OutlinedTextField(value = providerId, onValueChange = { providerId = it }, label = { Text(strings.providerId) }, singleLine = true)
                OutlinedTextField(value = workspacePath, onValueChange = { workspacePath = it }, label = { Text(strings.workspacePath) }, singleLine = true)
                OutlinedTextField(value = workspaceTitle, onValueChange = { workspaceTitle = it }, label = { Text(strings.workspaceTitle) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = host.isNotBlank(),
                onClick = {
                    onConfirm(name, host, port.toIntOrNull() ?: 8787, token, providerId, workspacePath, workspaceTitle)
                }
            ) {
                Text(strings.addAndConnect)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun NewSessionDialog(
    host: BridgeHost,
    strings: Strings,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var providerId by remember { mutableStateOf(host.providerId.ifBlank { "mock" }) }
    var workspacePath by remember { mutableStateOf(host.workspacePath) }
    var workspaceTitle by remember { mutableStateOf(host.workspaceTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newSession) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(host.name, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = providerId, onValueChange = { providerId = it }, label = { Text(strings.providerId) }, singleLine = true)
                OutlinedTextField(value = workspacePath, onValueChange = { workspacePath = it }, label = { Text(strings.workspacePath) }, singleLine = true)
                OutlinedTextField(value = workspaceTitle, onValueChange = { workspaceTitle = it }, label = { Text(strings.workspaceTitle) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(providerId, workspacePath, workspaceTitle) }) {
                Text(strings.create)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
