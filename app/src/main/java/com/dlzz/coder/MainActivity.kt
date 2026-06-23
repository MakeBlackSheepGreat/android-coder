package com.dlzz.coder

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlzz.coder.ui.chat.ChatScreen
import com.dlzz.coder.ui.files.FilePreviewScreen
import com.dlzz.coder.ui.main.MainScreen
import com.dlzz.coder.ui.navigation.Routes
import com.dlzz.coder.ui.theme.GlassTheme
import com.dlzz.coder.viewmodel.BridgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlassTheme {
                val bridgeViewModel: BridgeViewModel by viewModels()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = Routes.MAIN) {
                    composable(Routes.MAIN) {
                        MainScreen(
                            bridgeViewModel = bridgeViewModel,
                            onNavigateToChat = { hostId, sessionId -> navController.navigate(Routes.chat(hostId, sessionId)) },
                            onNavigateToFilePreview = { hostId, sessionId, path -> navController.navigate(Routes.filePreview(hostId, sessionId, path)) }
                        )
                    }
                    composable(Routes.CHAT) { backStackEntry ->
                        val hostId = Uri.decode(backStackEntry.arguments?.getString("hostId") ?: "")
                        val sessionId = Uri.decode(backStackEntry.arguments?.getString("sessionId") ?: "")
                        ChatScreen(
                            hostId = hostId,
                            sessionId = sessionId,
                            bridgeViewModel = bridgeViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.FILE_PREVIEW) { backStackEntry ->
                        val hostId = Uri.decode(backStackEntry.arguments?.getString("hostId") ?: "")
                        val sessionId = Uri.decode(backStackEntry.arguments?.getString("sessionId") ?: "")
                        val path = Uri.decode(backStackEntry.arguments?.getString("path") ?: "")
                        FilePreviewScreen(
                            hostId = hostId,
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
