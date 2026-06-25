package com.dlzz.coder.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import com.dlzz.coder.ui.components.MarkdownText
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.i18n.Strings
import com.dlzz.coder.ui.theme.accentColor
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
    DisposableEffect(Unit) {
        onDispose { chatViewModel.clear() }
    }
    val messages by chatViewModel.messages.collectAsState()
    val toolCalls by chatViewModel.toolCalls.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val language by bridgeViewModel.language.collectAsState()
    val pendingPermissions by bridgeViewModel.pendingPermissions.collectAsState()
    val aliases by bridgeViewModel.sessionAliases.collectAsState()
    val hostSessions by bridgeViewModel.hostSessions.collectAsState()
    val strings = AppStrings.of(language)
    val accent = accentColor()
    val isDark = isSystemInDarkTheme()

    val currentPermission = remember(pendingPermissions, hostId, sessionId) {
        pendingPermissions[bridgeViewModel.sessionEventKey(hostId, sessionId)]?.firstOrNull()
    }
    val sessionDisplayInfo = remember(hostSessions, aliases, sessionId) {
        val hs = hostSessions.firstOrNull { it.host.id == hostId && it.session.sessionId == sessionId }
        val alias = aliases[sessionId].orEmpty()
        val title = hs?.session?.displayName(alias) ?: sessionId.take(12)
        val meta = hs?.let { it.session.displayMeta(it.host.name) }.orEmpty()
        title to meta
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    // Auto-scroll only when user is near bottom
    LaunchedEffect(messages.size, toolCalls.size) {
        if (messages.isNotEmpty() || toolCalls.isNotEmpty()) {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = messages.size + toolCalls.size + 1 // +1 for header items
            val isNearBottom = totalItems - lastVisibleIndex <= 3

            if (isNearBottom) {
                scope.launch {
                    listState.animateScrollToItem((messages.size + toolCalls.size).coerceAtLeast(0))
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Glass top app bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        sessionDisplayInfo.first,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (sessionDisplayInfo.second.isNotEmpty()) {
                        Text(
                            sessionDisplayInfo.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
            }
        )

        // Message list with glass surface background
        LazyColumn(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (isLoading && messages.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accent)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                strings.loadingMessages,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (!isLoading && messages.isEmpty() && toolCalls.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                strings.emptyChat,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            items(messages) { msg ->
                MessageBubble(msg, isDark, accent)
            }
            if (toolCalls.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strings.toolCallsTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${toolCalls.size} 个工具",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(toolCalls, key = { it.id }) { tool ->
                    ToolCallCard(tool, strings)
                }
            }
        }

        // Glass input bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.messagePlaceholder) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.copy(alpha = 0.6f),
                    unfocusedBorderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)
                )
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        chatViewModel.sendMessage(hostId, sessionId, inputText)
                        inputText = ""
                        // Always scroll to bottom when user sends a message
                        scope.launch {
                            listState.animateScrollToItem(messages.size + toolCalls.size)
                        }
                    }
                },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.send, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.send)
            }
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
private fun MessageBubble(msg: ChatMessage, isDark: Boolean, accent: Color) {
    val isUser = msg.role == "user"
    val isSystem = msg.role == "system"
    val surfaceColor = when {
        isUser -> if (isDark) accent.copy(0.18f) else accent.copy(0.15f)
        isSystem -> if (isDark) Color(0xFFA32D2D).copy(0.15f) else Color(0xFFA32D2D).copy(0.1f)
        else -> if (isDark) Color.White.copy(0.08f) else Color.White.copy(0.6f)
    }
    val bubbleModifier = Modifier
        .glassBubble(cornerRadius = 16.dp, surfaceColor = surfaceColor)
        .animateContentSize()
        .padding(14.dp)
        .widthIn(max = 340.dp)

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(bubbleModifier) {
            Column {
                if (isSystem) {
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFFF6B6B) else Color(0xFFA32D2D)
                    )
                } else if (isUser) {
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color.Black
                    )
                } else {
                    // Assistant message - use Markdown
                    MarkdownText(
                        markdown = msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
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

    val hasOutput = tool.output.isNotEmpty()
    val outputLines = tool.output.lines()
    val shouldShowExpandButton = outputLines.size > 5 || tool.output.length > 500

    Column(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .glassCard(cornerRadius = 14.dp, blur = 3.dp, lensNear = 8.dp, lensFar = 16.dp)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                stateIcon,
                contentDescription = null,
                tint = stateColor,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    tool.name.ifBlank { "工具 #${tool.id.take(8)}" },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = stateColor
                )
            }
            if (hasOutput && shouldShowExpandButton) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (hasOutput) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(if (isSystemInDarkTheme()) 0.3f else 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        strings.toolOutput,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        tool.output,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!expanded && shouldShowExpandButton) {
                        Text(
                            "... 共 ${outputLines.size} 行",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
