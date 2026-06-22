package com.dlzz.coder.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

const val PROTOCOL_VERSION = "agent-bridge.v1"

object RequestType {
    const val CAPABILITIES_GET = "capabilities.get"
    const val PING = "bridge.ping"
    const val SESSION_CREATE = "session.create"
    const val SESSION_LIST = "session.list"
    const val SESSION_MESSAGES = "session.messages"
    const val SESSION_REVERT = "session.revert"
    const val SESSION_ABORT = "session.abort"
    const val MESSAGE_SEND = "message.send"
    const val PREVIEW_GET = "preview.get"
    const val PERMISSION_RESPOND = "permission.respond"
    const val REQUEST_RESPOND = "request.respond"
    const val PLAN_RESPOND = "plan.respond"
    const val WORKSPACE_CHANGES_GET = "workspace.changes.get"
    const val WORKSPACE_DIFF_GET = "workspace.diff.get"
    const val WORKSPACE_FILES_LIST = "workspace.files.list"
    const val WORKSPACE_FILE_GET = "workspace.file.get"
    const val WORKSPACE_FILE_DOWNLOAD = "workspace.file.download"
    const val ATTACHMENT_FILE_DOWNLOAD = "attachment.file.download"
    const val WORKSPACE_GIT_STAGE = "workspace.git.stage"
    const val WORKSPACE_GIT_UNSTAGE = "workspace.git.unstage"
    const val WORKSPACE_GIT_DISCARD = "workspace.git.discard"
    const val WORKSPACE_GIT_COMMIT = "workspace.git.commit"
}

object EventType {
    const val BRIDGE_CONNECTED = "bridge.connected"
    const val SESSION_CREATED = "session.created"
    const val SESSION_UPDATED = "session.updated"
    const val SESSION_MESSAGES = "session.messages"
    const val MESSAGE_DELTA = "message.delta"
    const val MESSAGE_COMPLETED = "message.completed"
    const val TOOL_STARTED = "tool.started"
    const val TOOL_OUTPUT = "tool.output"
    const val TOOL_COMPLETED = "tool.completed"
    const val PERMISSION_REQUESTED = "permission.requested"
    const val QUESTION_REQUESTED = "question.requested"
    const val PLAN_REQUESTED = "plan.requested"
    const val PLAN_UPDATED = "plan.updated"
    const val TODO_UPDATED = "todo.updated"
    const val PREVIEW_UPDATED = "preview.updated"
    const val WORKSPACE_CHANGES_UPDATED = "workspace.changes.updated"
    const val WORKSPACE_FILES_UPDATED = "workspace.files.updated"
    const val FILE_DOWNLOAD_READY = "file.download.ready"
    const val ERROR = "error"
}

@Serializable
data class ClientWireMessage(
    val id: String,
    val type: String,
    val payload: JsonObject? = null
)

@Serializable
data class ServerWireMessage(
    val id: String = "",
    val type: String = "",
    val ok: Boolean? = null,
    val event: String = "",
    val sessionId: String = "",
    val payload: JsonObject? = null,
    val error: ServerErrorBody? = null,
    val createdAt: Long = 0L
)

@Serializable
data class ServerErrorBody(
    val code: String = "",
    val message: String = ""
)

data class ProviderInfo(
    val id: String,
    val name: String,
    val status: String,
    val version: String
)

data class SessionInfo(
    val sessionId: String,
    val providerId: String,
    val workspacePath: String,
    val createdAt: Long
)

data class ChatMessage(
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolCall(
    val id: String,
    val name: String,
    val state: ToolState,
    val input: String = "",
    val output: String = ""
)

enum class ToolState { STARTED, RUNNING, COMPLETED, ERROR }

data class PermissionRequest(
    val requestId: String,
    val sessionId: String,
    val toolName: String,
    val description: String,
    val params: String = ""
)
