package com.dlzz.coder.ui.navigation

object Routes {
    const val CONNECTION = "connection"
    const val MAIN = "main"
    const val CHAT = "chat/{sessionId}"
    const val FILE_PREVIEW = "file_preview/{sessionId}/{path}"
    fun chat(sessionId: String) = "chat/$sessionId"
    fun filePreview(sessionId: String, path: String) = "file_preview/$sessionId/$path"
}
