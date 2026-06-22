package com.dlzz.coder.ui.connection

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.kyant.backdrop.backdrops.canvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.RoundedRectangle

@Composable
fun ConnectionSetupScreen(
    bridgeViewModel: BridgeViewModel,
    onConnected: () -> Unit
) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("8787") }
    var token by remember { mutableStateOf("") }
    val isConnected by bridgeViewModel.isConnected.collectAsState()

    LaunchedEffect(isConnected) {
        if (isConnected) onConnected()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = canvasBackdrop(),
                    shape = { RoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight(style = HighlightStyle.Default()) },
                    onDrawSurface = {
                        drawRect(
                            if (isSystemInDarkTheme())
                                androidx.compose.ui.graphics.Color(0xFF121212).copy(0.4f)
                            else
                                androidx.compose.ui.graphics.Color(0xFFFAFAFA).copy(0.4f)
                        )
                    }
                )
                .padding(24.dp)
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Connect to Agent Bridge", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Token") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    bridgeViewModel.connect(host, port.toIntOrNull() ?: 8787, token)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect")
            }
        }
    }
}
