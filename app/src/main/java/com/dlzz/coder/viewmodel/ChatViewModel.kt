package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    fun startListening(sessionId: String) {
        viewModelScope.launch {
            bridgeViewModel.sessionEvents.collect { sessionMap ->
                val sessionMsgs = sessionMap[sessionId] ?: return@collect
                for (msg in sessionMsgs) {
                    when (msg.event) {
                        "message.delta" -> {
                            val text = msg.payload?.get("text")?.let { it.toString().trim('"') } ?: ""
                            if (text.isNotEmpty()) appendAssistantDelta(text)
                        }
                        "tool.started" -> {
                            val toolId = msg.payload?.get("id")?.let { it.toString().trim('"') } ?: ""
                            val toolName = msg.payload?.get("name")?.let { it.toString().trim('"') } ?: ""
                            _toolCalls.value = _toolCalls.value + ToolCall(
                                id = toolId,
                                name = toolName,
                                state = ToolState.STARTED
                            )
                        }
                        "tool.completed" -> {
                            val toolId = msg.payload?.get("id")?.let { it.toString().trim('"') } ?: ""
                            _toolCalls.value = _toolCalls.value.map {
                                if (it.id == toolId) it.copy(state = ToolState.COMPLETED) else it
                            }
                        }
                        "error" -> {
                            val errorMsg = msg.error?.message ?: "Unknown error"
                            _messages.value = _messages.value + ChatMessage(role = "system", text = "Error: $errorMsg")
                        }
                    }
                }
            }
        }
    }
}
