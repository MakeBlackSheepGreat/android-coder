package com.dlzz.coder.bridge

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
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
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(tag, "Connected")
                _connected.tryEmit(true)
            }
            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<ServerWireMessage>(text)
                    _events.tryEmit(msg)
                } catch (e: Exception) {
                    Log.e(tag, "Parse error: ${e.message}")
                }
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "Failure: ${t.message}")
                _connected.tryEmit(false)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
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
            payload = json.encodeToJsonElement(payload) as JsonObject
        )
        webSocket?.send(json.encodeToString(ClientWireMessage.serializer(), msg))
        return id
    }

    fun disconnect() {
        webSocket?.close(1000, "client closing")
        webSocket = null
    }
}
