package com.dlzz.coder.viewmodel

import com.dlzz.coder.bridge.ToolState
import kotlinx.serialization.json.JsonArray
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

    @Test
    fun parseHistoryMessages_acceptsTextAndContentFields() {
        val payload = buildJsonObject {
            put("messages", JsonArray(listOf(
                buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("text", JsonPrimitive("hello"))
                    put("createdAt", JsonPrimitive(100L))
                },
                buildJsonObject {
                    put("role", JsonPrimitive("assistant"))
                    put("content", JsonPrimitive("world"))
                    put("createdAt", JsonPrimitive(200L))
                }
            )))
        }

        val messages = parseHistoryMessages(payload)

        assertEquals(2, messages.size)
        assertEquals("user", messages[0].role)
        assertEquals("hello", messages[0].text)
        assertEquals(100L, messages[0].timestamp)
        assertEquals("assistant", messages[1].role)
        assertEquals("world", messages[1].text)
        assertEquals(200L, messages[1].timestamp)
    }

    @Test
    fun parseHistoryMessages_skipsBlankNonSystemMessages() {
        val payload = buildJsonObject {
            put("messages", JsonArray(listOf(
                buildJsonObject {
                    put("role", JsonPrimitive("assistant"))
                    put("text", JsonPrimitive(""))
                },
                buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("text", JsonPrimitive(""))
                }
            )))
        }

        val messages = parseHistoryMessages(payload)

        assertEquals(1, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("", messages[0].text)
    }

    @Test
    fun parseHistoryToolCalls_mapsFallbackIdsAndStates() {
        val payload = buildJsonObject {
            put("toolCalls", JsonArray(listOf(
                buildJsonObject {
                    put("toolCallId", JsonPrimitive("tc-001"))
                    put("toolName", JsonPrimitive("read_file"))
                    put("state", JsonPrimitive("running"))
                    put("content", JsonPrimitive("partial"))
                },
                buildJsonObject {
                    put("id", JsonPrimitive("tc-002"))
                    put("name", JsonPrimitive("write_file"))
                    put("state", JsonPrimitive("failed"))
                    put("output", JsonPrimitive("denied"))
                }
            )))
        }

        val toolCalls = parseHistoryToolCalls(payload)

        assertEquals(2, toolCalls.size)
        assertEquals("tc-001", toolCalls[0].id)
        assertEquals("read_file", toolCalls[0].name)
        assertEquals(ToolState.RUNNING, toolCalls[0].state)
        assertEquals("partial", toolCalls[0].output)
        assertEquals("tc-002", toolCalls[1].id)
        assertEquals("write_file", toolCalls[1].name)
        assertEquals(ToolState.ERROR, toolCalls[1].state)
        assertEquals("denied", toolCalls[1].output)
    }
}
