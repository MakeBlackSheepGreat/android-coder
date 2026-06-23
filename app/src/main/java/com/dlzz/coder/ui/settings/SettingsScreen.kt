package com.dlzz.coder.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlzz.coder.ui.i18n.AppLanguage
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel

@Composable
fun SettingsScreen(bridgeViewModel: BridgeViewModel) {
    val hosts by bridgeViewModel.hosts.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val connectedCount = hosts.count { it.connected }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.settingsTitle, style = MaterialTheme.typography.headlineMedium)

        Box(
            Modifier
                .fillMaxWidth()
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
                .glassCard(cornerRadius = 16.dp)
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
