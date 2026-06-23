package com.dlzz.coder.bridge

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal fun JsonObject.stringValue(key: String): String? {
    return (this[key] as? JsonPrimitive)?.contentOrNull
}

internal fun JsonObject.longValue(key: String): Long? {
    return (this[key] as? JsonPrimitive)?.longOrNull
}

internal fun JsonObject.arrayValue(key: String): JsonArray? {
    return this[key] as? JsonArray
}

internal fun JsonObject.objectValue(key: String): JsonObject? {
    return this[key] as? JsonObject
}
