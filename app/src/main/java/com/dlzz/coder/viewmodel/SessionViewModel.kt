package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ServerWireMessage
import com.dlzz.coder.bridge.SessionInfo
import com.dlzz.coder.bridge.arrayValue
import com.dlzz.coder.bridge.longValue
import com.dlzz.coder.bridge.objectValue
import com.dlzz.coder.bridge.stringValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class SessionViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
    val sessions: StateFlow<List<SessionInfo>> = _sessions
    private var processedCount = 0

    init {
        viewModelScope.launch {
            bridgeViewModel.events.collect { messages ->
                messages.drop(processedCount).forEach(::handleMessage)
                processedCount = messages.size
            }
        }
    }

    fun createSession(providerId: String, workspacePath: String) {
        bridgeViewModel.send(RequestType.SESSION_CREATE, mapOf(
            "providerId" to providerId,
            "workspacePath" to workspacePath
        ))
    }

    fun listSessions() {
        bridgeViewModel.send(RequestType.SESSION_LIST)
    }

    private fun handleMessage(message: ServerWireMessage) {
        when {
            message.event == EventType.SESSION_CREATED ||
                message.event == EventType.SESSION_UPDATED -> {
                parseSession(message.payload, message.sessionId)?.let(::upsertSession)
            }
            message.type == RequestType.SESSION_CREATE -> {
                parseSession(message.payload, message.sessionId)?.let(::upsertSession)
            }
            message.type == RequestType.SESSION_LIST ||
                message.event == EventType.SESSION_MESSAGES -> {
                parseSessionList(message.payload).forEach(::upsertSession)
            }
        }
    }

    private fun parseSessionList(payload: JsonObject?): List<SessionInfo> {
        if (payload == null) return emptyList()
        val sessions = payload.arrayValue("sessions") ?: payload.arrayValue("items") ?: return emptyList()
        return sessions.mapNotNull { entry ->
            parseSession(entry as? JsonObject, fallbackSessionId = null)
        }
    }

    private fun parseSession(payload: JsonObject?, fallbackSessionId: String?): SessionInfo? {
        val sessionPayload = payload?.objectValue("session") ?: payload
        val sessionId = sessionPayload?.stringValue("sessionId")
            ?: sessionPayload?.stringValue("id")
            ?: fallbackSessionId
        if (sessionId.isNullOrBlank()) return null

        return SessionInfo(
            sessionId = sessionId,
            providerId = sessionPayload?.stringValue("providerId").orEmpty(),
            workspacePath = sessionPayload?.stringValue("workspacePath").orEmpty(),
            createdAt = sessionPayload?.longValue("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun upsertSession(session: SessionInfo) {
        val next = _sessions.value
            .filterNot { it.sessionId == session.sessionId }
            .plus(session)
            .sortedByDescending { it.activityAt() }
        _sessions.value = next
    }
}
