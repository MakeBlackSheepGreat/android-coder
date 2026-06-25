package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ServerWireMessage
import com.dlzz.coder.bridge.arrayValue
import com.dlzz.coder.bridge.objectValue
import com.dlzz.coder.bridge.stringValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class FileItem(val path: String, val type: String = "file")

data class FilePreview(val path: String, val content: String = "")

class FileViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _preview = MutableStateFlow<FilePreview?>(null)
    val preview: StateFlow<FilePreview?> = _preview

    private var activeHostId = ""
    private var activeSessionId = ""
    private val sessionEventCounts = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch {
            bridgeViewModel.sessionEvents.collect { sessionMap ->
                val key = sessionEventKey(activeHostId, activeSessionId)
                val messages = sessionMap[key].orEmpty()
                val lastProcessed = sessionEventCounts[key] ?: 0

                messages.drop(lastProcessed).forEach(::handleMessage)
                sessionEventCounts[key] = messages.size
            }
        }
    }

    fun listFiles(hostId: String, sessionId: String) {
        if (activeHostId != hostId || activeSessionId != sessionId) {
            // Switching to a new session, reset state
            activeHostId = hostId
            activeSessionId = sessionId
            _files.value = emptyList()
            _preview.value = null
        }
        bridgeViewModel.send(hostId, RequestType.WORKSPACE_FILES_LIST, mapOf("sessionId" to sessionId))
    }

    fun getFile(hostId: String, sessionId: String, path: String) {
        if (activeHostId != hostId || activeSessionId != sessionId) {
            activeHostId = hostId
            activeSessionId = sessionId
            _preview.value = null
        }
        bridgeViewModel.send(hostId, RequestType.WORKSPACE_FILE_GET, mapOf(
            "sessionId" to sessionId,
            "path" to path
        ))
    }

    private fun sessionEventKey(hostId: String, sessionId: String): String {
        return "$hostId|$sessionId"
    }

    private fun handleMessage(message: ServerWireMessage) {
        if (activeSessionId.isNotBlank() && message.sessionId.isNotBlank() && message.sessionId != activeSessionId) {
            return
        }
        when {
            message.event == EventType.WORKSPACE_FILES_UPDATED ||
                message.type == RequestType.WORKSPACE_FILES_LIST -> {
                parseFiles(message.payload).takeIf { it.isNotEmpty() }?.let { _files.value = it }
            }
            message.event == EventType.PREVIEW_UPDATED ||
                message.type == RequestType.WORKSPACE_FILE_GET -> {
                parsePreview(message.payload)?.let { _preview.value = it }
            }
        }
    }

    private fun parseFiles(payload: JsonObject?): List<FileItem> {
        if (payload == null) return emptyList()
        val files = payload.arrayValue("files")
            ?: payload.arrayValue("items")
            ?: payload.arrayValue("entries")
            ?: return emptyList()

        return files.mapNotNull { entry ->
            when (entry) {
                is JsonPrimitive -> entry.contentOrNull?.let { FileItem(path = it) }
                is JsonObject -> {
                    val path = entry.stringValue("path")
                        ?: entry.stringValue("name")
                        ?: entry.stringValue("relativePath")
                    val type = entry.stringValue("type")
                        ?: entry.stringValue("kind")
                        ?: "file"
                    path?.let { FileItem(path = it, type = type) }
                }
                else -> null
            }
        }
    }

    private fun parsePreview(payload: JsonObject?): FilePreview? {
        if (payload == null) return null
        val previewPayload = payload.objectValue("preview") ?: payload
        val path = previewPayload.stringValue("path") ?: previewPayload.stringValue("filePath") ?: return null
        val content = previewPayload.stringValue("content")
            ?: previewPayload.stringValue("text")
            ?: previewPayload.stringValue("data")
            ?: ""
        return FilePreview(path = path, content = content)
    }
}
