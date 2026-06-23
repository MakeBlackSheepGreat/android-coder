package com.dlzz.coder.ui.navigation

import android.net.Uri

object Routes {
    const val MAIN = "main"
    const val CHAT = "chat/{hostId}/{sessionId}"
    const val FILE_PREVIEW = "file_preview/{hostId}/{sessionId}?path={path}"
    fun chat(hostId: String, sessionId: String) = "chat/${Uri.encode(hostId)}/${Uri.encode(sessionId)}"
    fun filePreview(hostId: String, sessionId: String, path: String) =
        "file_preview/${Uri.encode(hostId)}/${Uri.encode(sessionId)}?path=${Uri.encode(path)}"
}
