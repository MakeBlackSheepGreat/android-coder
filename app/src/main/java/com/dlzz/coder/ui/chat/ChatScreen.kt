package com.dlzz.coder.ui.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.dlzz.coder.viewmodel.ChatViewModel
import com.kyant.backdrop.backdrops.canvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    sessionId: String,
    bridgeViewModel: BridgeViewModel,
    onBack: () -> Unit
) {
    val chatViewModel = remember { ChatViewModel(bridgeViewModel) }
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(sessionId.take(20) + "...") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    chatViewModel.sendMessage(sessionId, inputText)
                    inputText = ""
                    scope.launch { listState.animateScrollToItem(messages.size) }
                }
            }) { Text("Send") }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Box(
        Modifier
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
                        if (isUser)
                            Color(0xFF0091FF).copy(0.2f)
                        else if (isSystemInDarkTheme())
                            Color.White.copy(0.06f)
                        else
                            Color.White.copy(0.5f)
                    )
                }
            )
            .padding(12.dp)
            .widthIn(max = 300.dp)
    ) {
        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
    }
}
