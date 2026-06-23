package com.dlzz.coder.bridge

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonPayloadTest {

    private fun makeObject(vararg pairs: Pair<String, Any?>): JsonObject {
        return buildJsonObject {
            pairs.forEach { (key, value) ->
                when (value) {
                    null -> {}
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is JsonObject -> put(key, value)
                    is JsonArray -> put(key, value)
                }
            }
        }
    }

    @Test
    fun stringValue_returnsContentWhenPresent() {
        val obj = makeObject("name" to "read_file")
        assertEquals("read_file", obj.stringValue("name"))
    }

    @Test
    fun stringValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to "value")
        assertNull(obj.stringValue("name"))
    }

    @Test
    fun stringValue_returnsNullWhenNotPrimitive() {
        val nested = makeObject("nested" to "x")
        val obj = makeObject("name" to nested)
        assertNull(obj.stringValue("name"))
    }

    @Test
    fun longValue_parsesLongPrimitive() {
        val obj = makeObject("createdAt" to 1719000000000L)
        assertEquals(1719000000000L, obj.longValue("createdAt"))
    }

    @Test
    fun longValue_parsesIntPrimitive() {
        val obj = makeObject("count" to 42)
        assertEquals(42L, obj.longValue("count"))
    }

    @Test
    fun longValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to "value")
        assertNull(obj.longValue("createdAt"))
    }

    @Test
    fun arrayValue_returnsArrayWhenPresent() {
        val arr = JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b")))
        val obj = makeObject("items" to arr)
        val result = obj.arrayValue("items")
        assertEquals(2, result?.size)
    }

    @Test
    fun arrayValue_returnsNullWhenNotArray() {
        val obj = makeObject("items" to "not-an-array")
        assertNull(obj.arrayValue("items"))
    }

    @Test
    fun objectValue_returnsObjectWhenPresent() {
        val nested = makeObject("inner" to "value")
        val obj = makeObject("session" to nested)
        val result = obj.objectValue("session")
        assertEquals("value", result?.stringValue("inner"))
    }

    @Test
    fun objectValue_returnsNullWhenNotObject() {
        val obj = makeObject("session" to "string-value")
        assertNull(obj.objectValue("session"))
    }

    @Test
    fun objectValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to "value")
        assertNull(obj.objectValue("session"))
    }
}
