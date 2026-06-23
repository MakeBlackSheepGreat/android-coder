package com.dlzz.coder.viewmodel

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatViewModelToolCallIdTest {

    @Test
    fun toolCallId_prefersToolCallIdKey() {
        val obj = buildJsonObject {
            put("toolCallId", "tc-001")
            put("id", "fallback-001")
            put("toolUseId", "fallback-002")
            put("tool_use_id", "fallback-003")
            put("tool_call_id", "fallback-004")
        }
        assertEquals("tc-001", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToId() {
        val obj = buildJsonObject {
            put("id", "fallback-001")
            put("toolUseId", "fallback-002")
        }
        assertEquals("fallback-001", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToToolUseId() {
        val obj = buildJsonObject {
            put("toolUseId", "fallback-002")
        }
        assertEquals("fallback-002", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToSnakeCaseToolUseId() {
        val obj = buildJsonObject {
            put("tool_use_id", "fallback-003")
        }
        assertEquals("fallback-003", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToSnakeCaseToolCallId() {
        val obj = buildJsonObject {
            put("tool_call_id", "fallback-004")
        }
        assertEquals("fallback-004", obj.toolCallId())
    }

    @Test
    fun toolCallId_returnsNullWhenNoKeyPresent() {
        val obj = buildJsonObject {
            put("other", "value")
        }
        assertNull(obj.toolCallId())
    }

    @Test
    fun toolCallId_returnsNullForEmptyObject() {
        val obj = buildJsonObject {}
        assertNull(obj.toolCallId())
    }

    @Test
    fun toolCallId_ignoresNonPrimitiveValues() {
        val nested = buildJsonObject { put("inner", "x") }
        val obj = buildJsonObject {
            put("toolCallId", nested)
        }
        assertNull(obj.toolCallId())
    }
}
