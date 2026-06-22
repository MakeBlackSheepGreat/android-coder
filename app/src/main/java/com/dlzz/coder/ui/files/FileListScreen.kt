package com.dlzz.coder.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.FileViewModel
import com.kyant.backdrop.backdrops.canvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun FileListScreen(
    bridgeViewModel: BridgeViewModel,
    onFileClick: (String, String) -> Unit
) {
    val fileViewModel = remember { FileViewModel(bridgeViewModel) }
    val files by fileViewModel.files.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Workspace Files", style = MaterialTheme.typography.headlineMedium) }
        items(files.size) { idx ->
            val file = files[idx]
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = canvasBackdrop(),
                        shape = { RoundedRectangle(12.dp) },
                        effects = {
                            vibrancy()
                            blur(2f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(
                                if (isSystemInDarkTheme())
                                    androidx.compose.ui.graphics.Color.White.copy(0.05f)
                                else
                                    androidx.compose.ui.graphics.Color.White.copy(0.4f)
                            )
                        }
                    )
                    .clickable { onFileClick("", file.path) }
                    .padding(12.dp)
            ) {
                Text(file.path)
            }
        }
    }
}
