package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import com.dlzz.coder.bridge.RequestType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FileItem(val path: String, val type: String = "file")

data class FilePreview(val path: String, val content: String = "")

class FileViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _preview = MutableStateFlow<FilePreview?>(null)
    val preview: StateFlow<FilePreview?> = _preview

    fun listFiles(sessionId: String) {
        bridgeViewModel.send(RequestType.WORKSPACE_FILES_LIST, mapOf("sessionId" to sessionId))
    }

    fun getFile(sessionId: String, path: String) {
        bridgeViewModel.send(RequestType.WORKSPACE_FILE_GET, mapOf(
            "sessionId" to sessionId,
            "path" to path
        ))
    }
}
