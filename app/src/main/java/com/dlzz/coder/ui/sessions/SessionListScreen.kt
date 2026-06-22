package com.dlzz.coder.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.ServerWireMessage
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.SessionViewModel
import com.kyant.backdrop.backdrops.canvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun SessionListScreen(
    bridgeViewModel: BridgeViewModel,
    onSessionClick: (String) -> Unit
) {
    val sessionViewModel = remember { SessionViewModel(bridgeViewModel) }
    val events by bridgeViewModel.events.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Sessions", style = MaterialTheme.typography.headlineMedium)
        }
        items(events.size) { idx ->
            val msg = events[idx]
            SessionCard(msg, onClick = { onSessionClick(msg.sessionId) })
        }
    }
}

@Composable
private fun SessionCard(msg: ServerWireMessage, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark)
        Color.White.copy(0.08f)
    else
        Color.White.copy(0.6f)

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
                    drawRect(surfaceColor)
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(msg.sessionId, style = MaterialTheme.typography.bodyLarge)
            Text(msg.event, style = MaterialTheme.typography.bodySmall)
        }
    }
}
