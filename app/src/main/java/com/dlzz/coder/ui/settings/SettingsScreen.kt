package com.dlzz.coder.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.ui.i18n.AppLanguage
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun SettingsScreen(bridgeViewModel: BridgeViewModel) {
    val hosts by bridgeViewModel.hosts.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val isDark = isSystemInDarkTheme()
    val connectedCount = hosts.count { it.connected }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)

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
                        drawRect(
                            if (isDark)
                                androidx.compose.ui.graphics.Color.White.copy(0.06f)
                            else
                                androidx.compose.ui.graphics.Color.White.copy(0.5f)
                        )
                    }
                )
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
                .drawBackdrop(
                    backdrop = rememberCanvasBackdrop {},
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark)
                                androidx.compose.ui.graphics.Color.White.copy(0.06f)
                            else
                                androidx.compose.ui.graphics.Color.White.copy(0.5f)
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.language, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}
