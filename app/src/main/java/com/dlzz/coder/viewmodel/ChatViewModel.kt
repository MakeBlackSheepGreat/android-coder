package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _toolCalls = MutableStateFlow<List<ToolCall>>(emptyList())
    val toolCalls: StateFlow<List<ToolCall>> = _toolCalls

    fun sendMessage(sessionId: String, text: String) {
        _messages.value = _messages.value + ChatMessage(role = "user", text = text)
        bridgeViewModel.send(RequestType.MESSAGE_SEND, mapOf(
            "sessionId" to sessionId,
            "text" to text
        ))
    }

    fun appendAssistantDelta(text: String) {
        val msgs = _messages.value.toMutableList()
        val last = msgs.lastOrNull()
        if (last != null && last.role == "assistant") {
            msgs[msgs.lastIndex] = last.copy(text = last.text + text)
        } else {
            msgs.add(ChatMessage(role = "assistant", text = text))
        }
        _messages.value = msgs
    }
}
