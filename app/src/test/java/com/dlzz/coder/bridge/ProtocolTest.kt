package com.dlzz.coder.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {

    @Test
    fun protocolVersion_isAgentBridgeV1() {
        assertEquals("agent-bridge.v1", PROTOCOL_VERSION)
    }

    @Test
    fun requestTypes_containCoreOperations() {
        assertEquals("session.create", RequestType.SESSION_CREATE)
        assertEquals("session.list", RequestType.SESSION_LIST)
        assertEquals("message.send", RequestType.MESSAGE_SEND)
        assertEquals("workspace.files.list", RequestType.WORKSPACE_FILES_LIST)
        assertEquals("workspace.file.get", RequestType.WORKSPACE_FILE_GET)
        assertEquals("permission.respond", RequestType.PERMISSION_RESPOND)
    }

    @Test
    fun eventTypes_containCoreEvents() {
        assertEquals("session.created", EventType.SESSION_CREATED)
        assertEquals("message.delta", EventType.MESSAGE_DELTA)
        assertEquals("message.completed", EventType.MESSAGE_COMPLETED)
        assertEquals("tool.started", EventType.TOOL_STARTED)
        assertEquals("tool.output", EventType.TOOL_OUTPUT)
        assertEquals("tool.completed", EventType.TOOL_COMPLETED)
        assertEquals("permission.requested", EventType.PERMISSION_REQUESTED)
        assertEquals("workspace.files.updated", EventType.WORKSPACE_FILES_UPDATED)
        assertEquals("preview.updated", EventType.PREVIEW_UPDATED)
        assertEquals("error", EventType.ERROR)
    }

    @Test
    fun toolState_hasAllFourStates() {
        val states = ToolState.entries
        assertEquals(4, states.size)
        assertTrue(states.contains(ToolState.STARTED))
        assertTrue(states.contains(ToolState.RUNNING))
        assertTrue(states.contains(ToolState.COMPLETED))
        assertTrue(states.contains(ToolState.ERROR))
    }

    @Test
    fun permissionRequest_hasRequiredFields() {
        val req = PermissionRequest(
            requestId = "req-001",
            sessionId = "sess-001",
            toolName = "write_file",
            description = "Write to /tmp/test.txt",
            params = "{}"
        )
        assertEquals("req-001", req.requestId)
        assertEquals("sess-001", req.sessionId)
        assertEquals("write_file", req.toolName)
        assertEquals("Write to /tmp/test.txt", req.description)
        assertEquals("{}", req.params)
    }

    @Test
    fun permissionRequest_paramsDefaultsToEmpty() {
        val req = PermissionRequest(
            requestId = "req-001",
            sessionId = "sess-001",
            toolName = "write_file",
            description = "desc"
        )
        assertEquals("", req.params)
    }

    @Test
    fun bridgeHost_addressCombinesHostAndPort() {
        val host = BridgeHost(
            id = "h1",
            name = "test",
            host = "192.168.1.100",
            port = 8787
        )
        assertEquals("192.168.1.100:8787", host.address)
    }

    @Test
    fun bridgeHost_defaultsPortTo8787() {
        val host = BridgeHost(id = "h1", name = "test", host = "localhost")
        assertEquals(8787, host.port)
    }

    @Test
    fun bridgeHost_defaultsConnectedToFalse() {
        val host = BridgeHost(id = "h1", name = "test", host = "localhost")
        assertFalse(host.connected)
    }

    @Test
    fun sessionInfo_fileBasename_extractsLastPathSegment() {
        val s = SessionInfo(
            sessionId = "s1",
            workspacePath = "/home/user/projects/my-app"
        )
        assertEquals("my-app", s.fileBasename())
    }

    @Test
    fun sessionInfo_fileBasename_handlesTrailingSlash() {
        val s = SessionInfo(sessionId = "s1", workspacePath = "/home/user/projects/my-app/")
        assertEquals("my-app", s.fileBasename())
    }

    @Test
    fun sessionInfo_fileBasename_handlesWindowsPath() {
        val s = SessionInfo(sessionId = "s1", workspacePath = "C:\\Users\\foo\\my-project")
        assertEquals("my-project", s.fileBasename())
    }

    @Test
    fun sessionInfo_fileBasename_emptyWhenNoPath() {
        val s = SessionInfo(sessionId = "s1")
        assertEquals("", s.fileBasename())
    }

    @Test
    fun displayName_prefersLocalAlias() {
        val s = SessionInfo(sessionId = "s1", title = "server-title", workspaceTitle = "ws-title")
        assertEquals("my-alias", s.displayName("my-alias"))
    }

    @Test
    fun displayName_fallsBackToTitle() {
        val s = SessionInfo(sessionId = "s1", title = "server-title", workspaceTitle = "ws-title")
        assertEquals("server-title", s.displayName())
    }

    @Test
    fun displayName_fallsBackToWorkspaceTitle() {
        val s = SessionInfo(sessionId = "s1", workspaceTitle = "ws-title")
        assertEquals("ws-title", s.displayName())
    }

    @Test
    fun displayName_fallsBackToFileBasename() {
        val s = SessionInfo(sessionId = "s1", workspacePath = "/home/user/my-project")
        assertEquals("my-project", s.displayName())
    }

    @Test
    fun displayName_fallsBackToSessionIdPrefix() {
        val s = SessionInfo(sessionId = "abcdefgh12345678")
        assertEquals("abcdefgh1234", s.displayName())
    }

    @Test
    fun displayMeta_combinesAllFields() {
        val s = SessionInfo(
            sessionId = "s1",
            workspacePath = "/projects/my-app",
            modelId = "gpt-4",
            messageCount = 5
        )
        val meta = s.displayMeta("MyHost")
        assertTrue(meta.contains("MyHost"))
        assertTrue(meta.contains("my-app"))
        assertTrue(meta.contains("gpt-4"))
        assertTrue(meta.contains("5msgs"))
    }

    @Test
    fun displayMeta_omitsConfiguredModelPlaceholder() {
        val s = SessionInfo(
            sessionId = "s1",
            workspacePath = "/projects/my-app",
            modelId = "configured",
            messageCount = 5
        )
        val meta = s.displayMeta("MyHost")
        assertTrue(meta.contains("MyHost"))
        assertTrue(meta.contains("my-app"))
        assertFalse(meta.contains("configured"))
        assertTrue(meta.contains("5msgs"))
    }

    @Test
    fun relativeTime_justNowForRecent() {
        val now = 1_000_000L
        val s = SessionInfo(sessionId = "s1", createdAt = now - 30_000L)
        assertEquals("just now", s.relativeTime(now))
    }

    @Test
    fun relativeTime_minutesForOlder() {
        val now = 1_000_000L
        val s = SessionInfo(sessionId = "s1", createdAt = now - 5 * 60_000L)
        assertEquals("5m", s.relativeTime(now))
    }

    @Test
    fun activityAt_prefersUpdatedAt() {
        val s = SessionInfo(sessionId = "s1", createdAt = 100L, updatedAt = 500L)
        assertEquals(500L, s.activityAt())
    }

    @Test
    fun activityAt_fallsBackToCreatedAt() {
        val s = SessionInfo(sessionId = "s1", createdAt = 100L, updatedAt = 0L)
        assertEquals(100L, s.activityAt())
    }

    @Test
    fun relativeTime_usesUpdatedAtWhenPresent() {
        val now = 1_000_000L
        val s = SessionInfo(
            sessionId = "s1",
            createdAt = now - 5 * 60_000L,
            updatedAt = now - 30_000L
        )
        assertEquals("just now", s.relativeTime(now))
    }
}
