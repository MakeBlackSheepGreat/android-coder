package com.dlzz.coder.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.AgentBridgeClient
import com.dlzz.coder.bridge.BridgeHost
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.HostSession
import com.dlzz.coder.bridge.PermissionRequest
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.ServerWireMessage
import com.dlzz.coder.bridge.SessionInfo
import com.dlzz.coder.bridge.arrayValue
import com.dlzz.coder.bridge.longValue
import com.dlzz.coder.bridge.objectValue
import com.dlzz.coder.bridge.stringValue
import com.dlzz.coder.ui.i18n.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.TimeUnit

class BridgeViewModel(application: Application) : AndroidViewModel(application) {
    data class ScanState(
        val running: Boolean = false,
        val scanned: Int = 0,
        val total: Int = 0,
        val added: Int = 0,
        val message: String = ""
    )

    private data class HostRuntime(
        val client: AgentBridgeClient,
        val connectedJob: Job,
        val eventsJob: Job
    )

    private data class PendingRequest(val hostId: String, val type: String)

    private val prefs = application.getSharedPreferences("bridge_hosts", Application.MODE_PRIVATE)
    private val aliasPrefs = application.getSharedPreferences("session_aliases", Application.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val runtimes = java.util.concurrent.ConcurrentHashMap<String, HostRuntime>()
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, PendingRequest>()

    private val _hosts = MutableStateFlow<List<BridgeHost>>(emptyList())
    val hosts: StateFlow<List<BridgeHost>> = _hosts

    private val _language = MutableStateFlow(AppLanguage.ZH)
    val language: StateFlow<AppLanguage> = _language

    private val _activeHostId = MutableStateFlow("")
    val activeHostId: StateFlow<String> = _activeHostId

    private val _eventsByHost = MutableStateFlow<Map<String, List<ServerWireMessage>>>(emptyMap())
    val eventsByHost: StateFlow<Map<String, List<ServerWireMessage>>> = _eventsByHost

    private val _sessionsByHost = MutableStateFlow<Map<String, List<SessionInfo>>>(emptyMap())
    val sessionsByHost: StateFlow<Map<String, List<SessionInfo>>> = _sessionsByHost

    private val _hostSessions = MutableStateFlow<List<HostSession>>(emptyList())
    val hostSessions: StateFlow<List<HostSession>> = _hostSessions

    private val _sessionEvents = MutableStateFlow<Map<String, List<ServerWireMessage>>>(emptyMap())
    val sessionEvents: StateFlow<Map<String, List<ServerWireMessage>>> = _sessionEvents

    private val _sessionAliases = MutableStateFlow<Map<String, String>>(loadSessionAliases())
    val sessionAliases: StateFlow<Map<String, String>> = _sessionAliases

    private val _pendingPermissions = MutableStateFlow<Map<String, List<PermissionRequest>>>(emptyMap())
    val pendingPermissions: StateFlow<Map<String, List<PermissionRequest>>> = _pendingPermissions

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _events = MutableStateFlow<List<ServerWireMessage>>(emptyList())
    val events: StateFlow<List<ServerWireMessage>> = _events

    private val _scanState = MutableStateFlow(ScanState())
    val scanState: StateFlow<ScanState> = _scanState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        _language.value = AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, AppLanguage.ZH.code).orEmpty())
        val savedHosts = loadHosts()
        _hosts.value = savedHosts
        _activeHostId.value = savedHosts.firstOrNull()?.id.orEmpty()
        savedHosts.forEach { connectHost(it.id) }
    }

    fun addHost(
        name: String,
        host: String,
        port: Int,
        token: String,
        providerId: String = "",
        workspacePath: String = "",
        workspaceTitle: String = "",
        connectNow: Boolean = true
    ): String {
        val trimmedHost = host.trim()
        val hostId = UUID.randomUUID().toString()
        val displayName = name.trim().ifBlank {
            workspaceTitle.ifBlank { "$trimmedHost:$port" }
        }
        val bridgeHost = BridgeHost(
            id = hostId,
            name = displayName,
            host = trimmedHost,
            port = port,
            token = token.trim(),
            providerId = providerId.trim(),
            workspacePath = workspacePath.trim(),
            workspaceTitle = workspaceTitle.trim()
        )
        _hosts.value = _hosts.value + bridgeHost
        _activeHostId.value = hostId
        persistHosts()
        if (connectNow) connectHost(hostId)
        return hostId
    }

    fun scanAndAddHosts(
        targetText: String,
        port: Int = 8787,
        token: String = "",
        providerId: String = "",
        workspacePath: String = "",
        workspaceTitle: String = ""
    ) {
        if (_scanState.value.running) return
        viewModelScope.launch {
            _scanState.value = ScanState(running = true, message = "正在准备扫描")
            val candidates = withContext(Dispatchers.IO) {
                buildScanCandidates(targetText, port)
            }
            if (candidates.isEmpty()) {
                _scanState.value = ScanState(message = "没有可扫描的目标")
                return@launch
            }

            _scanState.value = ScanState(running = true, total = candidates.size, message = "正在扫描")
            val found = withContext(Dispatchers.IO) {
                scanHealthEndpoints(candidates)
            }
            var added = 0
            for (candidate in found) {
                val exists = _hosts.value.any { it.host == candidate.host && it.port == candidate.port }
                if (!exists) {
                    addHost(
                        name = candidate.host + ":" + candidate.port,
                        host = candidate.host,
                        port = candidate.port,
                        token = token,
                        providerId = providerId,
                        workspacePath = workspacePath,
                        workspaceTitle = workspaceTitle,
                        connectNow = token.isNotBlank()
                    )
                    added += 1
                }
            }
            _scanState.value = ScanState(
                running = false,
                scanned = candidates.size,
                total = candidates.size,
                added = added,
                message = if (added > 0) "已添加 $added 台主机" else "未发现新的 Bridge 主机"
            )
        }
    }

    fun addHostFromQr(rawValue: String): Result<String> {
        return runCatching {
            val parsed = parseConnectionPayload(rawValue.trim())
            addHost(
                name = parsed.name,
                host = parsed.host,
                port = parsed.port,
                token = parsed.token,
                providerId = parsed.providerId,
                workspacePath = parsed.workspacePath,
                workspaceTitle = parsed.workspaceTitle
            )
        }
    }

    fun selectHost(hostId: String) {
        if (_hosts.value.any { it.id == hostId }) {
            _activeHostId.value = hostId
        }
    }

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun connectHost(hostId: String) {
        val bridgeHost = _hosts.value.firstOrNull { it.id == hostId } ?: return
        disconnectHost(hostId, updateState = false)

        val client = AgentBridgeClient(bridgeHost.host, bridgeHost.port, bridgeHost.token)
        client.connect()
        val connectedJob = viewModelScope.launch {
            client.connected.collect { connected ->
                updateHost(hostId) { it.copy(connected = connected) }
                updateGlobalConnectionState()
                if (connected) {
                    listSessions(hostId)
                }
            }
        }
        val eventsJob = viewModelScope.launch {
            client.events.collect { message ->
                handleHostMessage(hostId, message)
            }
        }
        runtimes[hostId] = HostRuntime(client, connectedJob, eventsJob)
    }

    fun disconnectHost(hostId: String, updateState: Boolean = true) {
        val runtime = runtimes.remove(hostId)
        runtime?.connectedJob?.cancel()
        runtime?.eventsJob?.cancel()
        runtime?.client?.disconnect()
        if (updateState) {
            updateHost(hostId) { it.copy(connected = false) }
            updateGlobalConnectionState()
        }
    }

    fun removeHost(hostId: String) {
        disconnectHost(hostId)
        _hosts.value = _hosts.value.filterNot { it.id == hostId }
        _eventsByHost.value = _eventsByHost.value - hostId
        _sessionsByHost.value = _sessionsByHost.value - hostId
        _sessionEvents.value = _sessionEvents.value.filterKeys { !it.startsWith("$hostId|") }
        if (_activeHostId.value == hostId) {
            _activeHostId.value = _hosts.value.firstOrNull()?.id.orEmpty()
        }
        persistHosts()
        rebuildHostSessions()
    }

    fun listSessions(hostId: String) {
        send(hostId, RequestType.SESSION_LIST)
    }

    fun refreshAllSessions() {
        val connected = _hosts.value.filter { it.connected }
        if (connected.isEmpty()) return
        _isRefreshing.value = true
        connected.forEach { listSessions(it.id) }
        viewModelScope.launch {
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun createSession(hostId: String, providerId: String = "", workspacePath: String = "", workspaceTitle: String = "") {
        val bridgeHost = _hosts.value.firstOrNull { it.id == hostId } ?: return
        val resolvedProviderId = providerId.ifBlank { bridgeHost.providerId.ifBlank { "mock" } }
        val resolvedWorkspacePath = workspacePath.ifBlank { bridgeHost.workspacePath }
        val resolvedWorkspaceTitle = workspaceTitle.ifBlank { bridgeHost.workspaceTitle }
        send(
            hostId = hostId,
            type = RequestType.SESSION_CREATE,
            payload = mapOf(
                "providerId" to resolvedProviderId,
                "workspacePath" to resolvedWorkspacePath,
                "workspaceTitle" to resolvedWorkspaceTitle
            )
        )
    }

    fun send(hostId: String, type: String, payload: Map<String, Any?> = emptyMap()): String {
        val runtime = runtimes[hostId] ?: run {
            com.dlzz.coder.debug.LogCollector.w("BridgeVM", "send: no runtime for hostId=$hostId type=$type")
            return ""
        }
        val id = runtime.client.send(type, payload)
        if (id.isNotBlank()) {
            pendingRequests[id] = PendingRequest(hostId, type)
            com.dlzz.coder.debug.LogCollector.d("BridgeVM", "send type=$type reqId=$id host=${hostId.take(8)}")
        }
        return id
    }

    fun send(type: String, payload: Map<String, Any?> = emptyMap()): String {
        val hostId = _activeHostId.value.ifBlank { _hosts.value.firstOrNull { it.connected }?.id.orEmpty() }
        return if (hostId.isNotBlank()) send(hostId, type, payload) else ""
    }

    fun respondPermission(hostId: String, requestId: String, allowed: Boolean) {
        send(hostId, RequestType.PERMISSION_RESPOND, mapOf(
            "requestId" to requestId,
            "allowed" to allowed
        ))
        _pendingPermissions.value = _pendingPermissions.value.mapValues { (_, list) ->
            list.filterNot { it.requestId == requestId }
        }
    }

    fun connect(host: String, port: Int, token: String) {
        addHost(name = "$host:$port", host = host, port = port, token = token)
    }

    fun disconnect() {
        runtimes.keys.toList().forEach { disconnectHost(it) }
    }

    fun sessionMessages(hostId: String, sessionId: String): List<ServerWireMessage> {
        return _sessionEvents.value[sessionEventKey(hostId, sessionId)] ?: emptyList()
    }

    fun sessionMessages(sessionId: String): List<ServerWireMessage> {
        val hostId = _activeHostId.value
        return sessionMessages(hostId, sessionId)
    }

    fun sessionEventKey(hostId: String, sessionId: String): String = "$hostId|$sessionId"

    private fun handleHostMessage(hostId: String, message: ServerWireMessage) {
        val pending = if (message.id.isNotBlank()) pendingRequests.remove(message.id) else null
        val effectiveType = pending?.type ?: message.type

        _events.value = _events.value + message
        _eventsByHost.value = _eventsByHost.value.toMutableMap().also { map ->
            map[hostId] = map[hostId].orEmpty() + message
        }

        if (message.sessionId.isNotBlank()) {
            val key = sessionEventKey(hostId, message.sessionId)
            _sessionEvents.value = _sessionEvents.value.toMutableMap().also { map ->
                map[key] = map[key].orEmpty() + message
            }
        }

        when {
            effectiveType == RequestType.SESSION_LIST -> {
                val sessions = parseSessionList(message.payload)
                _sessionsByHost.value = _sessionsByHost.value.toMutableMap().also { it[hostId] = sessions }
                rebuildHostSessions()
            }
            effectiveType == RequestType.SESSION_CREATE ||
                message.event == EventType.SESSION_CREATED ||
                message.event == EventType.SESSION_UPDATED -> {
                parseSession(message.payload, message.sessionId)?.let { upsertSession(hostId, it) }
            }
            message.event == EventType.SESSION_MESSAGES -> {
                parseSessionList(message.payload).forEach { upsertSession(hostId, it) }
            }
            message.event == EventType.PERMISSION_REQUESTED -> {
                parsePermissionRequest(message.payload, message.sessionId)?.let { req ->
                    val key = sessionEventKey(hostId, req.sessionId)
                    _pendingPermissions.value = _pendingPermissions.value.toMutableMap().also { map ->
                        map[key] = map[key].orEmpty() + req
                    }
                }
            }
        }
    }

    private fun parseSessionList(payload: JsonObject?): List<SessionInfo> {
        if (payload == null) return emptyList()
        val sessions = payload.arrayValue("sessions") ?: payload.arrayValue("items") ?: return emptyList()
        return sessions.mapNotNull { parseSession(it as? JsonObject, fallbackSessionId = null) }
    }

    private fun parseSession(payload: JsonObject?, fallbackSessionId: String?): SessionInfo? {
        val sessionPayload = payload?.objectValue("session") ?: payload
        val sessionId = sessionPayload?.stringValue("sessionId")
            ?: sessionPayload?.stringValue("id")
            ?: sessionPayload?.stringValue("remoteSessionId")
            ?: fallbackSessionId
        if (sessionId.isNullOrBlank()) return null

        val title = sessionPayload?.stringValue("title")
            ?: sessionPayload?.stringValue("displayTitle")
            ?: sessionPayload?.stringValue("threadName")
            ?: sessionPayload?.stringValue("conversationTitle")
            ?: sessionPayload?.stringValue("name")
            ?: sessionPayload?.stringValue("summary")
            ?: sessionPayload?.stringValue("initialPrompt")
            ?: sessionPayload?.stringValue("prompt")
            ?: sessionPayload?.stringValue("firstMessage")
            ?: sessionPayload?.stringValue("message")
            ?: ""

        val messageCount = sessionPayload?.stringValue("messageCount")?.toIntOrNull()
            ?: sessionPayload?.stringValue("messages")?.toIntOrNull()
            ?: sessionPayload?.stringValue("count")?.toIntOrNull()
            ?: 0

        return SessionInfo(
            sessionId = sessionId,
            providerId = sessionPayload?.stringValue("providerId").orEmpty(),
            workspacePath = sessionPayload?.stringValue("workspacePath").orEmpty(),
            workspaceTitle = sessionPayload?.stringValue("workspaceTitle").orEmpty(),
            title = title,
            modelId = sessionPayload?.stringValue("modelId")
                ?: sessionPayload?.stringValue("model")
                ?: "",
            branchName = sessionPayload?.stringValue("branchName")
                ?: sessionPayload?.stringValue("branch")
                ?: "",
            messageCount = messageCount,
            createdAt = sessionPayload?.longValue("createdAt") ?: System.currentTimeMillis(),
            updatedAt = sessionPayload?.longValue("updatedAt")
                ?: sessionPayload?.longValue("modifiedAt")
                ?: 0L,
            status = sessionPayload?.stringValue("status").orEmpty()
        )
    }

    fun renameSession(sessionId: String, alias: String) {
        val current = _sessionAliases.value.toMutableMap()
        if (alias.isBlank()) {
            current.remove(sessionId)
        } else {
            current[sessionId] = alias.trim()
        }
        _sessionAliases.value = current
        persistSessionAliases(current)
        rebuildHostSessions()
    }

    fun sessionAlias(sessionId: String): String = _sessionAliases.value[sessionId].orEmpty()

    private fun loadSessionAliases(): Map<String, String> {
        val raw = aliasPrefs.getString(KEY_ALIASES, null) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), raw)
        }.getOrDefault(emptyMap())
    }

    private fun persistSessionAliases(map: Map<String, String>) {
        aliasPrefs.edit()
            .putString(KEY_ALIASES, json.encodeToString(MapSerializer(String.serializer(), String.serializer()), map))
            .apply()
    }

    private fun parsePermissionRequest(payload: JsonObject?, fallbackSessionId: String): PermissionRequest? {
        val p = payload ?: return null
        val requestId = p.stringValue("requestId")
            ?: p.stringValue("id")
            ?: p.stringValue("permissionId")
            ?: return null
        val sessionId = p.stringValue("sessionId")?.ifBlank { null } ?: fallbackSessionId
        val toolName = p.stringValue("toolName")
            ?: p.stringValue("tool")
            ?: p.stringValue("name")
            ?: ""
        val description = p.stringValue("description")
            ?: p.stringValue("message")
            ?: p.stringValue("reason")
            ?: ""
        val params = p.stringValue("params")
            ?: p.stringValue("input")
            ?: p.stringValue("args")
            ?: ""
        return PermissionRequest(
            requestId = requestId,
            sessionId = sessionId,
            toolName = toolName,
            description = description,
            params = params
        )
    }

    private fun upsertSession(hostId: String, session: SessionInfo) {
        _sessionsByHost.value = _sessionsByHost.value.toMutableMap().also { map ->
            val next = map[hostId].orEmpty()
                .filterNot { it.sessionId == session.sessionId }
                .plus(session)
                .sortedByDescending { it.activityAt() }
            map[hostId] = next
        }
        rebuildHostSessions()
    }

    private fun rebuildHostSessions() {
        val hostsById = _hosts.value.associateBy { it.id }
        _hostSessions.value = _sessionsByHost.value.flatMap { (hostId, sessions) ->
            val bridgeHost = hostsById[hostId] ?: return@flatMap emptyList()
            sessions.map { HostSession(bridgeHost, it) }
        }.sortedByDescending { it.session.activityAt() }
    }

    private fun updateHost(hostId: String, update: (BridgeHost) -> BridgeHost) {
        _hosts.value = _hosts.value.map { if (it.id == hostId) update(it) else it }
        persistHosts()
        rebuildHostSessions()
    }

    private fun updateGlobalConnectionState() {
        _isConnected.value = _hosts.value.any { it.connected }
    }

    private fun loadHosts(): List<BridgeHost> {
        val raw = prefs.getString(KEY_HOSTS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(BridgeHost.serializer()), raw)
                .map { it.copy(connected = false) }
        }.getOrDefault(emptyList())
    }

    private fun persistHosts() {
        val persisted = _hosts.value.map { it.copy(connected = false) }
        prefs.edit()
            .putString(KEY_HOSTS, json.encodeToString(ListSerializer(BridgeHost.serializer()), persisted))
            .apply()
    }

    override fun onCleared() {
        runtimes.values.forEach {
            it.connectedJob.cancel()
            it.eventsJob.cancel()
            it.client.disconnect()
        }
        runtimes.clear()
    }

    companion object {
        private const val KEY_HOSTS = "hosts"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_ALIASES = "aliases"
        private const val PROTOCOL_VERSION = "agent-bridge.v1"
        private const val MAX_SCAN_CANDIDATES = 512
        private val scanHttpClient = OkHttpClient.Builder()
            .connectTimeout(450, TimeUnit.MILLISECONDS)
            .readTimeout(700, TimeUnit.MILLISECONDS)
            .callTimeout(900, TimeUnit.MILLISECONDS)
            .build()

        private data class ScanCandidate(val host: String, val port: Int)

        internal data class ParsedConnection(
            val name: String,
            val host: String,
            val port: Int,
            val token: String,
            val providerId: String,
            val workspacePath: String,
            val workspaceTitle: String
        )

        internal fun parseConnectionPayload(rawValue: String): ParsedConnection {
            if (rawValue.startsWith("{")) {
                val obj = JSONObject(rawValue)
                return parseEndpoint(
                    endpoint = obj.optString("endpoint"),
                    token = obj.optString("token"),
                    providerId = obj.optString("providerId"),
                    workspacePath = obj.optString("workspacePath"),
                    workspaceTitle = obj.optString("workspaceTitle")
                )
            }

            val uri = Uri.parse(rawValue)
            if (uri.scheme == "ngf-agent-bridge") {
                return parseEndpoint(
                    endpoint = uri.getQueryParameter("endpoint").orEmpty(),
                    token = uri.getQueryParameter("token").orEmpty(),
                    providerId = uri.getQueryParameter("providerId").orEmpty(),
                    workspacePath = uri.getQueryParameter("workspacePath").orEmpty(),
                    workspaceTitle = uri.getQueryParameter("workspaceTitle").orEmpty()
                )
            }

            if (uri.scheme == "ws" || uri.scheme == "wss") {
                return parseEndpoint(
                    endpoint = rawValue,
                    token = uri.getQueryParameter("token").orEmpty(),
                    providerId = uri.getQueryParameter("providerId").orEmpty(),
                    workspacePath = uri.getQueryParameter("workspacePath").orEmpty(),
                    workspaceTitle = uri.getQueryParameter("workspaceTitle").orEmpty()
                )
            }

            throw IllegalArgumentException("不支持的二维码内容")
        }

        internal fun parseEndpoint(
            endpoint: String,
            token: String,
            providerId: String,
            workspacePath: String,
            workspaceTitle: String
        ): ParsedConnection {
            val uri = Uri.parse(endpoint)
            val host = uri.host.orEmpty()
            if (host.isBlank()) throw IllegalArgumentException("二维码缺少主机地址")
            val port = if (uri.port > 0) uri.port else 8787
            val name = workspaceTitle.ifBlank { "$host:$port" }
            return ParsedConnection(
                name = name,
                host = host,
                port = port,
                token = token,
                providerId = providerId,
                workspacePath = workspacePath,
                workspaceTitle = workspaceTitle
            )
        }

        private fun buildScanCandidates(targetText: String, port: Int): List<ScanCandidate> {
            val targets = targetText
                .split(',', '\n', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val hosts = if (targets.isEmpty()) {
                localInterfacePrefixes().flatMap { prefix ->
                    (1..254).map { "$prefix.$it" }
                }
            } else {
                targets.flatMap { expandScanTarget(it) }
            }

            return hosts
                .distinct()
                .take(MAX_SCAN_CANDIDATES)
                .map { ScanCandidate(it, port) }
        }

        private fun localInterfacePrefixes(): List<String> {
            val result = mutableSetOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val parts = address.hostAddress.orEmpty().split('.')
                        if (parts.size == 4) {
                            result += parts.take(3).joinToString(".")
                        }
                    }
                }
            }
            return result.toList()
        }

        internal fun expandScanTarget(target: String): List<String> {
            val normalized = target.removePrefix("http://").removePrefix("https://").substringBefore(':')
            if (normalized.endsWith(".*")) {
                val prefix = normalized.removeSuffix(".*")
                return (1..254).map { "$prefix.$it" }
            }
            if (normalized.count { it == '.' } == 2) {
                return (1..254).map { "$normalized.$it" }
            }
            if (normalized.contains('/')) {
                return expandCidr(normalized)
            }
            return listOf(normalized)
        }

        internal fun expandCidr(cidr: String): List<String> {
            val parts = cidr.split('/')
            if (parts.size != 2) return emptyList()
            val base = ipv4ToLong(parts[0]) ?: return emptyList()
            val prefixLength = parts[1].toIntOrNull()?.coerceIn(0, 32) ?: return emptyList()
            val mask = if (prefixLength == 0) 0L else (-1L shl (32 - prefixLength)) and 0xffffffffL
            val network = base and mask
            val count = 1L shl (32 - prefixLength)
            val first = if (count <= 2) 0L else 1L
            val lastExclusive = if (count <= 2) count else count - 1
            val result = mutableListOf<String>()
            var offset = first
            while (offset < lastExclusive && result.size < MAX_SCAN_CANDIDATES) {
                result += longToIpv4(network + offset)
                offset += 1
            }
            return result
        }

        internal fun ipv4ToLong(value: String): Long? {
            val parts = value.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.size != 4 || parts.any { it !in 0..255 }) return null
            return parts.fold(0L) { acc, part -> (acc shl 8) + part }
        }

        internal fun longToIpv4(value: Long): String {
            return listOf(
                (value shr 24) and 255,
                (value shr 16) and 255,
                (value shr 8) and 255,
                value and 255
            ).joinToString(".")
        }

        private suspend fun scanHealthEndpoints(candidates: List<ScanCandidate>): List<ScanCandidate> {
            return coroutineScope {
                candidates.chunked(32).flatMap { chunk ->
                    chunk.map { candidate ->
                        async {
                            if (isBridgeHealthy(candidate)) candidate else null
                        }
                    }.awaitAll().filterNotNull()
                }
            }
        }

        private fun isBridgeHealthy(candidate: ScanCandidate): Boolean {
            val request = Request.Builder()
                .url("http://${candidate.host}:${candidate.port}/health")
                .get()
                .build()
            return runCatching {
                scanHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return false
                    val body = response.body?.string().orEmpty()
                    val parsed = JSONObject(body)
                    parsed.optBoolean("ok") && parsed.optString("protocolVersion") == PROTOCOL_VERSION
                }
            }.getOrDefault(false)
        }
    }
}
