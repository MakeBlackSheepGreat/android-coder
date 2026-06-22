package com.dlzz.coder.ui.files

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.FileViewModel

@Composable
fun FilePreviewScreen(
    sessionId: String,
    filePath: String,
    bridgeViewModel: BridgeViewModel,
    onBack: () -> Unit
) {
    val fileViewModel = remember { FileViewModel(bridgeViewModel) }
    val preview by fileViewModel.preview.collectAsState()

    LaunchedEffect(sessionId, filePath) {
        fileViewModel.getFile(sessionId, filePath)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(filePath.takeLast(30)) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
        Box(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(
                preview?.content ?: "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
