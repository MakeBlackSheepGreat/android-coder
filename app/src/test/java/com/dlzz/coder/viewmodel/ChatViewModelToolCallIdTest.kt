package com.dlzz.coder.viewmodel

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatViewModelToolCallIdTest {

    @Test
    fun toolCallId_prefersToolCallIdKey() {
        val obj = buildJsonObject {
            put("toolCallId", JsonPrimitive("tc-001"))
            put("id", JsonPrimitive("fallback-001"))
            put("toolUseId", JsonPrimitive("fallback-002"))
            put("tool_use_id", JsonPrimitive("fallback-003"))
            put("tool_call_id", JsonPrimitive("fallback-004"))
        }
        assertEquals("tc-001", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToId() {
        val obj = buildJsonObject {
            put("id", JsonPrimitive("fallback-001"))
            put("toolUseId", JsonPrimitive("fallback-002"))
        }
        assertEquals("fallback-001", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToToolUseId() {
        val obj = buildJsonObject {
            put("toolUseId", JsonPrimitive("fallback-002"))
        }
        assertEquals("fallback-002", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToSnakeCaseToolUseId() {
        val obj = buildJsonObject {
            put("tool_use_id", JsonPrimitive("fallback-003"))
        }
        assertEquals("fallback-003", obj.toolCallId())
    }

    @Test
    fun toolCallId_fallsBackToSnakeCaseToolCallId() {
        val obj = buildJsonObject {
            put("tool_call_id", JsonPrimitive("fallback-004"))
        }
        assertEquals("fallback-004", obj.toolCallId())
    }

    @Test
    fun toolCallId_returnsNullWhenNoKeyPresent() {
        val obj = buildJsonObject {
            put("other", JsonPrimitive("value"))
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
        val nested = buildJsonObject { put("inner", JsonPrimitive("x")) }
        val obj = buildJsonObject {
            put("toolCallId", nested)
        }
        assertNull(obj.toolCallId())
    }
}
