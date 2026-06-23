package com.dlzz.coder.bridge

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonPayloadTest {

    private fun makeObject(vararg pairs: Pair<String, JsonElement?>): JsonObject {
        return buildJsonObject {
            pairs.forEach { (key, value) ->
                if (value != null) put(key, value)
            }
        }
    }

    @Test
    fun stringValue_returnsContentWhenPresent() {
        val obj = makeObject("name" to JsonPrimitive("read_file"))
        assertEquals("read_file", obj.stringValue("name"))
    }

    @Test
    fun stringValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to JsonPrimitive("value"))
        assertNull(obj.stringValue("name"))
    }

    @Test
    fun stringValue_returnsNullWhenNotPrimitive() {
        val nested = makeObject("nested" to JsonPrimitive("x"))
        val obj = makeObject("name" to nested)
        assertNull(obj.stringValue("name"))
    }

    @Test
    fun longValue_parsesLongPrimitive() {
        val obj = makeObject("createdAt" to JsonPrimitive(1719000000000L))
        assertEquals(1719000000000L, obj.longValue("createdAt"))
    }

    @Test
    fun longValue_parsesIntPrimitive() {
        val obj = makeObject("count" to JsonPrimitive(42))
        assertEquals(42L, obj.longValue("count"))
    }

    @Test
    fun longValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to JsonPrimitive("value"))
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
        val obj = makeObject("items" to JsonPrimitive("not-an-array"))
        assertNull(obj.arrayValue("items"))
    }

    @Test
    fun objectValue_returnsObjectWhenPresent() {
        val nested = makeObject("inner" to JsonPrimitive("value"))
        val obj = makeObject("session" to nested)
        val result = obj.objectValue("session")
        assertEquals("value", result?.stringValue("inner"))
    }

    @Test
    fun objectValue_returnsNullWhenNotObject() {
        val obj = makeObject("session" to JsonPrimitive("string-value"))
        assertNull(obj.objectValue("session"))
    }

    @Test
    fun objectValue_returnsNullWhenAbsent() {
        val obj = makeObject("other" to JsonPrimitive("value"))
        assertNull(obj.objectValue("session"))
    }
}
