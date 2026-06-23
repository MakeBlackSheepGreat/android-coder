package com.dlzz.coder.bridge

import kotlinx.serialization.Serializable

@Serializable
data class BridgeHost(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 8787,
    val token: String = "",
    val providerId: String = "",
    val workspacePath: String = "",
    val workspaceTitle: String = "",
    val connected: Boolean = false
) {
    val address: String
        get() = "$host:$port"
}

data class HostSession(
    val host: BridgeHost,
    val session: SessionInfo
)
