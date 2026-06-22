package com.dlzz.coder.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.kyant.backdrop.backdrops.canvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun SettingsScreen(bridgeViewModel: BridgeViewModel) {
    val isConnected by bridgeViewModel.isConnected.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = canvasBackdrop(),
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isSystemInDarkTheme())
                                androidx.compose.ui.graphics.Color.White.copy(0.06f)
                            else
                                androidx.compose.ui.graphics.Color.White.copy(0.5f)
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connection Status: ${if (isConnected) "Connected" else "Disconnected"}")
                Text("Protocol: agent-bridge.v1")
                if (isConnected) {
                    Button(onClick = { bridgeViewModel.disconnect() }) { Text("Disconnect") }
                }
            }
        }
    }
}
