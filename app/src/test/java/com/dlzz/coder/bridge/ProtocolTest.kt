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
}
