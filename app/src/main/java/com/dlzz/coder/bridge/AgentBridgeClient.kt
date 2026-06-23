package com.dlzz.coder.bridge

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AgentBridgeClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 8787,
    private val token: String = ""
) {
    private val tag = "AgentBridge"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val nextId = AtomicInteger(0)
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _events = MutableSharedFlow<ServerWireMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerWireMessage> = _events

    private val _connected = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1)
    val connected: SharedFlow<Boolean> = _connected

    fun connect() {
        val wsUrl = "ws://$host:$port/ws?token=$token"
        val request = Request.Builder().url(wsUrl).header("Authorization", "Bearer $token").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Connected")
                _connected.tryEmit(true)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<ServerWireMessage>(text)
                    _events.tryEmit(msg)
                } catch (e: Exception) {
                    Log.e(tag, "Parse error: ${e.message}")
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Failure: ${t.message}")
                _connected.tryEmit(false)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "Closed: $reason")
                _connected.tryEmit(false)
            }
        })
    }

    fun send(requestType: String, payload: Map<String, Any?> = emptyMap()): String {
        val id = "req-${nextId.incrementAndGet()}"
        val msg = ClientWireMessage(
            id = id,
            type = requestType,
            payload = encodePayload(payload)
        )
        webSocket?.send(json.encodeToString(ClientWireMessage.serializer(), msg))
        return id
    }

    private fun encodePayload(payload: Map<String, Any?>): JsonObject {
        return buildJsonObject {
            payload.forEach { (key, value) ->
                put(key, encodeValue(value))
            }
        }
    }

    private fun encodeValue(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Map<*, *> -> buildJsonObject {
                value.forEach { (key, nestedValue) ->
                    if (key is String) {
                        put(key, encodeValue(nestedValue))
                    }
                }
            }
            is Iterable<*> -> JsonArray(value.map { encodeValue(it) })
            is Array<*> -> JsonArray(value.map { encodeValue(it) })
            else -> JsonPrimitive(value.toString())
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "client closing")
        webSocket = null
    }
}
