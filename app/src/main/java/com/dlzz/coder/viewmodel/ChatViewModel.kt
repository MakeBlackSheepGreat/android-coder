package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import com.dlzz.coder.bridge.stringValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _toolCalls = MutableStateFlow<List<ToolCall>>(emptyList())
    val toolCalls: StateFlow<List<ToolCall>> = _toolCalls
    private var listenJob: Job? = null
    private var processedCount = 0

    fun sendMessage(hostId: String, sessionId: String, text: String) {
        _messages.value = _messages.value + ChatMessage(role = "user", text = text)
        bridgeViewModel.send(hostId, RequestType.MESSAGE_SEND, mapOf(
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

    fun startListening(hostId: String, sessionId: String) {
        listenJob?.cancel()
        processedCount = 0
        listenJob = viewModelScope.launch {
            bridgeViewModel.sessionEvents.collect { sessionMap ->
                val sessionMsgs = sessionMap[bridgeViewModel.sessionEventKey(hostId, sessionId)].orEmpty()
                for (msg in sessionMsgs.drop(processedCount)) {
                    when (msg.event) {
                        EventType.MESSAGE_DELTA -> {
                            val text = msg.payload?.stringValue("text").orEmpty()
                            if (text.isNotEmpty()) appendAssistantDelta(text)
                        }
                        EventType.TOOL_STARTED -> {
                            val toolId = msg.payload?.toolCallId().orEmpty()
                            val toolName = msg.payload?.stringValue("name").orEmpty()
                            _toolCalls.value = _toolCalls.value + ToolCall(
                                id = toolId,
                                name = toolName,
                                state = ToolState.STARTED
                            )
                        }
                        EventType.TOOL_OUTPUT -> {
                            val toolId = msg.payload?.toolCallId().orEmpty()
                            val output = msg.payload?.stringValue("output")
                                ?: msg.payload?.stringValue("text")
                                ?: msg.payload?.stringValue("content")
                                ?: ""
                            _toolCalls.value = _toolCalls.value.map {
                                if (it.id == toolId) {
                                    it.copy(
                                        state = ToolState.RUNNING,
                                        output = it.output + output
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        EventType.TOOL_COMPLETED -> {
                            val toolId = msg.payload?.toolCallId().orEmpty()
                            _toolCalls.value = _toolCalls.value.map {
                                if (it.id == toolId) {
                                    val output = msg.payload?.stringValue("output")
                                        ?: msg.payload?.stringValue("text")
                                        ?: msg.payload?.stringValue("content")
                                        ?: it.output
                                    it.copy(state = ToolState.COMPLETED, output = output)
                                } else {
                                    it
                                }
                            }
                        }
                        EventType.ERROR -> {
                            val errorMsg = msg.error?.message ?: "Unknown error"
                            _messages.value = _messages.value + ChatMessage(role = "system", text = "错误：$errorMsg")
                        }
                    }
                }
                processedCount = sessionMsgs.size
            }
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
    }
}

internal fun kotlinx.serialization.json.JsonObject.toolCallId(): String? {
    return stringValue("toolCallId")
        ?: stringValue("id")
        ?: stringValue("toolUseId")
        ?: stringValue("tool_use_id")
        ?: stringValue("tool_call_id")
}
