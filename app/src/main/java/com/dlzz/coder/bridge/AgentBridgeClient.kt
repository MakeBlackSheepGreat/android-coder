package com.dlzz.coder.bridge

import android.util.Log
import com.dlzz.coder.debug.LogCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicBoolean
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
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<ServerWireMessage>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerWireMessage> = _events

    private val _connected = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 1)
    val connected: SharedFlow<Boolean> = _connected

    private val shouldReconnect = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectAttempts = 0
    private val maxReconnectDelay = 30_000L // 30 seconds max

    fun connect() {
        shouldReconnect.set(true)
        attemptConnect()
    }

    private fun attemptConnect() {
        if (isConnecting.getAndSet(true)) {
            return
        }

        val wsUrl = "ws://$host:$port/ws?token=$token"
        val request = Request.Builder().url(wsUrl).header("Authorization", "Bearer $token").build()
        LogCollector.i(tag, "Connecting to $wsUrl (attempt ${reconnectAttempts + 1})")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                LogCollector.i(tag, "Connected to $host:$port")
                isConnecting.set(false)
                reconnectAttempts = 0
                _connected.tryEmit(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = json.decodeFromString<ServerWireMessage>(text)
                    if (msg.event.isNotBlank() && msg.event != "bridge.connected" && msg.event != "bridge.ping") {
                        LogCollector.d(tag, "Recv event=${msg.event} session=${msg.sessionId.take(20)} ok=${msg.ok}")
                    }
                    _events.tryEmit(msg)
                } catch (e: Exception) {
                    LogCollector.e(tag, "Parse error: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LogCollector.e(tag, "Connection failure: ${t.message}", t)
                isConnecting.set(false)
                _connected.tryEmit(false)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                LogCollector.w(tag, "Closed: code=$code reason=$reason")
                isConnecting.set(false)
                _connected.tryEmit(false)
                if (code != 1000) { // 1000 = normal closure
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect.get()) {
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // Exponential backoff: 1s, 2s, 4s, 8s, 16s, up to 30s
            val delay = minOf((1000L * (1 shl reconnectAttempts)), maxReconnectDelay)
            LogCollector.d(tag, "Reconnecting in ${delay}ms")
            delay(delay)

            if (shouldReconnect.get() && isActive) {
                reconnectAttempts++
                attemptConnect()
            }
        }
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
        shouldReconnect.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "client closing")
        webSocket = null
        _connected.tryEmit(false)
    }
}
