package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.ChatMessage
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ToolCall
import com.dlzz.coder.bridge.ToolState
import com.dlzz.coder.bridge.arrayValue
import com.dlzz.coder.bridge.longValue
import com.dlzz.coder.debug.LogCollector
import com.dlzz.coder.bridge.stringValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject

class ChatViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _toolCalls = MutableStateFlow<List<ToolCall>>(emptyList())
    val toolCalls: StateFlow<List<ToolCall>> = _toolCalls

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val messageMutex = Mutex()
    private var listenJob: Job? = null
    private var processedCount = 0
    private var historyLoaded = false

    fun sendMessage(hostId: String, sessionId: String, text: String) {
        viewModelScope.launch {
            messageMutex.withLock {
                _messages.value = _messages.value + ChatMessage(role = "user", text = text)
            }
        }
        bridgeViewModel.send(hostId, RequestType.MESSAGE_SEND, mapOf(
            "sessionId" to sessionId,
            "text" to text
        ))
    }

    suspend fun appendAssistantDelta(text: String) {
        messageMutex.withLock {
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

    fun startListening(hostId: String, sessionId: String) {
        listenJob?.cancel()
        processedCount = 0
        historyLoaded = false
        _messages.value = emptyList()
        _toolCalls.value = emptyList()
        _isLoading.value = true
        LogCollector.i("ChatVM", "startListening hostId=${hostId.take(8)} sessionId=${sessionId.take(20)}")

        // Request historical messages from server
        val reqId = bridgeViewModel.send(hostId, RequestType.SESSION_MESSAGES, mapOf(
            "sessionId" to sessionId
        ))
        LogCollector.d("ChatVM", "Sent session.messages reqId=$reqId")

        listenJob = viewModelScope.launch {
            val historyTimeoutJob = launch {
                delay(15_000L) // Increased timeout to 15 seconds
                if (!historyLoaded) {
                    LogCollector.w("ChatVM", "session.messages timed out for session=${sessionId.take(20)}")
                    _isLoading.value = false
                }
            }
            try {
                bridgeViewModel.sessionEvents.collect { sessionMap ->
                    val sessionMsgs = sessionMap[bridgeViewModel.sessionEventKey(hostId, sessionId)].orEmpty()
                    for (msg in sessionMsgs.drop(processedCount)) {
                        when (msg.event) {
                            EventType.SESSION_MESSAGES -> {
                                if (!historyLoaded) {
                                    val payload = msg.payload
                                    LogCollector.i("ChatVM", "SESSION_MESSAGES event received, keys=${payload?.keys}")
                                    if (payload != null) {
                                        val historyMessages = parseHistoryMessages(payload)
                                        val historyToolCalls = parseHistoryToolCalls(payload)
                                        LogCollector.d("ChatVM", "Parsed ${historyMessages.size} messages, ${historyToolCalls.size} toolCalls")
                                        if (historyMessages.isNotEmpty()) {
                                            _messages.value = historyMessages
                                        }
                                        if (historyToolCalls.isNotEmpty()) {
                                            _toolCalls.value = historyToolCalls
                                        }
                                    }
                                    historyLoaded = true
                                    _isLoading.value = false
                                    historyTimeoutJob.cancel()
                                }
                            }
                            EventType.MESSAGE_DELTA -> {
                                val text = msg.payload?.stringValue("text").orEmpty()
                                if (text.isNotEmpty()) appendAssistantDelta(text)
                            }
                            EventType.MESSAGE_COMPLETED -> {
                                // Message generation finished
                            }
                            EventType.TOOL_STARTED -> {
                                val toolId = msg.payload?.toolCallId().orEmpty()
                                val toolName = msg.payload?.stringValue("name").orEmpty()
                                if (toolId.isNotBlank()) {
                                    _toolCalls.value = _toolCalls.value + ToolCall(
                                        id = toolId,
                                        name = toolName,
                                        state = ToolState.STARTED
                                    )
                                }
                            }
                            EventType.TOOL_OUTPUT -> {
                                val toolId = msg.payload?.toolCallId().orEmpty()
                                val output = msg.payload?.stringValue("output")
                                    ?: msg.payload?.stringValue("text")
                                    ?: msg.payload?.stringValue("content")
                                    ?: ""
                                _toolCalls.value = _toolCalls.value.map {
                                    if (it.id == toolId) {
                                        it.copy(state = ToolState.RUNNING, output = it.output + output)
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
                                LogCollector.e("ChatVM", "Error event: $errorMsg")
                                messageMutex.withLock {
                                    _messages.value = _messages.value + ChatMessage(role = "system", text = "错误：$errorMsg")
                                }
                                _isLoading.value = false
                            }
                        }
                    }
                    processedCount = sessionMsgs.size
                }
            } finally {
                historyTimeoutJob.cancel()
            }
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
    }

    fun clear() {
        listenJob?.cancel()
    }
}

internal fun parseHistoryMessages(payload: JsonObject): List<ChatMessage> {
    val messagesArray = payload.arrayValue("messages") ?: return emptyList()
    return messagesArray.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val role = obj.stringValue("role") ?: return@mapNotNull null
        val text = obj.stringValue("text") ?: obj.stringValue("content") ?: ""
        if (text.isBlank() && role != "system") return@mapNotNull null
        val timestamp = obj.longValue("createdAt") ?: System.currentTimeMillis()
        ChatMessage(role = role, text = text, timestamp = timestamp)
    }
}

internal fun parseHistoryToolCalls(payload: JsonObject): List<ToolCall> {
    val toolCallsArray = payload.arrayValue("toolCalls") ?: return emptyList()
    return toolCallsArray.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj.stringValue("id") ?: obj.stringValue("toolCallId") ?: return@mapNotNull null
        val name = obj.stringValue("name") ?: obj.stringValue("toolName") ?: ""
        val stateStr = obj.stringValue("state") ?: "completed"
        val state = when (stateStr.lowercase()) {
            "started" -> ToolState.STARTED
            "running" -> ToolState.RUNNING
            "error", "failed" -> ToolState.ERROR
            else -> ToolState.COMPLETED
        }
        val output = obj.stringValue("output") ?: obj.stringValue("text") ?: obj.stringValue("content") ?: ""
        ToolCall(id = id, name = name, state = state, output = output)
    }
}

internal fun JsonObject.toolCallId(): String? {
    return stringValue("toolCallId")
        ?: stringValue("id")
        ?: stringValue("toolUseId")
        ?: stringValue("tool_use_id")
        ?: stringValue("tool_call_id")
}
