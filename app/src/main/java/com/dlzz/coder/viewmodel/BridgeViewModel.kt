package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlzz.coder.bridge.AgentBridgeClient
import com.dlzz.coder.bridge.EventType
import com.dlzz.coder.bridge.ServerWireMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BridgeViewModel : ViewModel() {
    private var client = AgentBridgeClient()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _events = MutableStateFlow<List<ServerWireMessage>>(emptyList())
    val events: StateFlow<List<ServerWireMessage>> = _events

    fun connect(host: String, port: Int, token: String) {
        client = AgentBridgeClient(host, port, token)
        client.connect()
        viewModelScope.launch {
            client.connected.collect { connected ->
                _isConnected.value = connected
            }
        }
        viewModelScope.launch {
            client.events.collect { msg ->
                _events.value = _events.value + msg
            }
        }
    }

    fun send(type: String, payload: Map<String, Any?> = emptyMap()): String {
        return client.send(type, payload)
    }

    fun disconnect() {
        client.disconnect()
        _isConnected.value = false
    }

    override fun onCleared() {
        client.disconnect()
    }
}
