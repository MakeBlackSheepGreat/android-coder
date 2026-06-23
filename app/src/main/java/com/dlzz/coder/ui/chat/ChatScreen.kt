package com.dlzz.coder.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.i18n.Strings
import com.dlzz.coder.ui.theme.glassBubble
import com.dlzz.coder.ui.theme.glassCard
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    hostId: String,
    sessionId: String,
    bridgeViewModel: BridgeViewModel,
    onBack: () -> Unit
) {
    val chatViewModel = remember { ChatViewModel(bridgeViewModel) }
    LaunchedEffect(hostId, sessionId) {
        chatViewModel.startListening(hostId, sessionId)
    }
    val messages by chatViewModel.messages.collectAsState()
    val toolCalls by chatViewModel.toolCalls.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val pendingPermissions by bridgeViewModel.pendingPermissions.collectAsState()
    val strings = AppStrings.of(language)
    val currentPermission = remember(pendingPermissions, hostId, sessionId) {
        pendingPermissions[bridgeViewModel.sessionEventKey(hostId, sessionId)]?.firstOrNull()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, toolCalls.size) {
        if (messages.isNotEmpty() || toolCalls.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem((messages.size + toolCalls.size).coerceAtLeast(0)) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(sessionId.take(20) + "...") },
            navigationIcon = { TextButton(onClick = onBack) { Text(strings.back) } }
        )
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (toolCalls.isNotEmpty()) {
                item {
                    Text(
                        strings.toolCallsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(toolCalls, key = { it.id }) { tool ->
                    ToolCallCard(tool, strings)
                }
            }
        }
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.messagePlaceholder) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    chatViewModel.sendMessage(hostId, sessionId, inputText)
                    inputText = ""
                    scope.launch { listState.animateScrollToItem(messages.size) }
                }
            }) { Text(strings.send) }
        }
    }

    if (currentPermission != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(strings.permissionRequestTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.permissionRequestDesc(currentPermission.toolName, currentPermission.description))
                    if (currentPermission.params.isNotEmpty()) {
                        Text(
                            currentPermission.params,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    bridgeViewModel.respondPermission(hostId, currentPermission.requestId, true)
                }) { Text(strings.permissionAllow) }
            },
            dismissButton = {
                TextButton(onClick = {
                    bridgeViewModel.respondPermission(hostId, currentPermission.requestId, false)
                }) { Text(strings.permissionDeny) }
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    val isDark = isSystemInDarkTheme()
    val surfaceColor = when {
        isUser -> if (isDark) Color(0xFF0091FF).copy(0.18f) else Color(0xFF0088FF).copy(0.2f)
        else -> if (isDark) Color.White.copy(0.06f) else Color.White.copy(0.5f)
    }
    Box(
        Modifier
            .glassBubble(cornerRadius = 12.dp, surfaceColor = surfaceColor)
            .padding(12.dp)
            .widthIn(max = 300.dp)
    ) {
        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ToolCallCard(tool: ToolCall, strings: Strings) {
    var expanded by remember { mutableStateOf(false) }

    val (stateLabel, stateIcon, stateColor) = when (tool.state) {
        ToolState.STARTED -> Triple(strings.toolStarted, Icons.Default.Schedule, Color(0xFF854F0B))
        ToolState.RUNNING -> Triple(strings.toolRunning, Icons.Default.PlayArrow, Color(0xFF185FA5))
        ToolState.COMPLETED -> Triple(strings.toolCompleted, Icons.Default.Check, Color(0xFF0F6E56))
        ToolState.ERROR -> Triple(strings.toolError, Icons.Default.Error, Color(0xFFA32D2D))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 10.dp, blur = 2.dp, lensNear = 6.dp, lensFar = 12.dp)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(stateIcon, contentDescription = null, tint = stateColor, modifier = Modifier.size(16.dp))
            Text(tool.name.ifBlank { tool.id.take(8) }, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.weight(1f))
            Text(stateLabel, style = MaterialTheme.typography.labelSmall, color = stateColor)
        }
        if (tool.output.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                strings.toolOutput,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                tool.output,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextButton(onClick = { expanded = false }) {
                    Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
