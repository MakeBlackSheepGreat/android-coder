package com.dlzz.coder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlzz.coder.ui.chat.ChatScreen
import com.dlzz.coder.ui.connection.ConnectionSetupScreen
import com.dlzz.coder.ui.files.FileListScreen
import com.dlzz.coder.ui.files.FilePreviewScreen
import com.dlzz.coder.ui.main.MainScreen
import com.dlzz.coder.ui.navigation.Routes
import com.dlzz.coder.ui.sessions.SessionListScreen
import com.dlzz.coder.ui.settings.SettingsScreen
import com.dlzz.coder.ui.theme.GlassTheme
import com.dlzz.coder.viewmodel.BridgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassTheme {
                val bridgeViewModel: BridgeViewModel by viewModels()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.CONNECTION) {
                    composable(Routes.CONNECTION) {
                        ConnectionSetupScreen(
                            bridgeViewModel = bridgeViewModel,
                            onConnected = { navController.navigate(Routes.MAIN) { popUpTo(0) } }
                        )
                    }
                    composable(Routes.MAIN) {
                        MainScreen(
                            bridgeViewModel = bridgeViewModel,
                            onNavigateToChat = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                            onNavigateToFilePreview = { sessionId, path -> navController.navigate(Routes.filePreview(sessionId, path)) }
                        )
                    }
                    composable(Routes.CHAT) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        ChatScreen(
                            sessionId = sessionId,
                            bridgeViewModel = bridgeViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.FILE_PREVIEW) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        val path = backStackEntry.arguments?.getString("path") ?: ""
                        FilePreviewScreen(
                            sessionId = sessionId,
                            filePath = path,
                            bridgeViewModel = bridgeViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
