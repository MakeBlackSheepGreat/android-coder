package com.dlzz.coder.viewmodel

import androidx.lifecycle.ViewModel
import com.dlzz.coder.bridge.RequestType
import com.dlzz.coder.bridge.SessionInfo

class SessionViewModel(private val bridgeViewModel: BridgeViewModel) : ViewModel() {
    val sessions = bridgeViewModel.events

    fun createSession(providerId: String, workspacePath: String) {
        bridgeViewModel.send(RequestType.SESSION_CREATE, mapOf(
            "providerId" to providerId,
            "workspacePath" to workspacePath
        ))
    }

    fun listSessions() {
        bridgeViewModel.send(RequestType.SESSION_LIST)
    }
}
