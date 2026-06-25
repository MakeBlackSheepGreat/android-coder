package com.dlzz.coder.ui.files

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    hostId: String,
    sessionId: String,
    filePath: String,
    bridgeViewModel: BridgeViewModel,
    onBack: () -> Unit
) {
    val fileViewModel = remember { FileViewModel(bridgeViewModel) }
    val preview by fileViewModel.preview.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)

    LaunchedEffect(hostId, sessionId, filePath) {
        fileViewModel.getFile(hostId, sessionId, filePath)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    filePath,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
            }
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .glassCard(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Text(
                preview?.content ?: strings.loading,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
