package com.dlzz.coder.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dlzz.coder.ui.files.FileListScreen
import com.dlzz.coder.ui.hosts.HostListScreen
import com.dlzz.coder.ui.i18n.AppStrings
import com.dlzz.coder.ui.sessions.SessionListScreen
import com.dlzz.coder.ui.settings.SettingsScreen
import com.dlzz.coder.ui.theme.accentColor
import com.dlzz.coder.ui.theme.glassBottomBar
import com.dlzz.coder.ui.theme.glassTabIndicator
import com.dlzz.coder.ui.theme.rememberLayerBackdrop
import com.dlzz.coder.viewmodel.BridgeViewModel

enum class MainTab { HOSTS, SESSIONS, FILES, SETTINGS }

@Composable
fun MainScreen(
    bridgeViewModel: BridgeViewModel,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToFilePreview: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = MainTab.entries
    val backdrop = rememberLayerBackdrop()
    val language by bridgeViewModel.language.collectAsState()
    val strings = AppStrings.of(language)
    val tabLabels = listOf(strings.hostsTab, strings.sessionsTab, strings.filesTab, strings.settingsTab)

    Scaffold(
        bottomBar = {
            GlassBottomBar(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = tabLabels,
                backdrop = backdrop
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (
                    slideInHorizontally(animationSpec = tween(260)) { width -> direction * width / 5 } +
                        fadeIn(animationSpec = tween(220))
                    ).togetherWith(
                    slideOutHorizontally(animationSpec = tween(220)) { width -> -direction * width / 5 } +
                        fadeOut(animationSpec = tween(160))
                ).using(SizeTransform(clip = false))
            },
            label = "main-tab-transition",
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { tabIndex ->
            Box(Modifier.fillMaxSize()) {
                when (tabs[tabIndex]) {
                    MainTab.HOSTS -> HostListScreen(
                        bridgeViewModel = bridgeViewModel
                    )
                    MainTab.SESSIONS -> SessionListScreen(
                        bridgeViewModel = bridgeViewModel,
                        onSessionClick = onNavigateToChat
                    )
                    MainTab.FILES -> FileListScreen(
                        bridgeViewModel = bridgeViewModel,
                        onFileClick = onNavigateToFilePreview
                    )
                    MainTab.SETTINGS -> SettingsScreen(bridgeViewModel = bridgeViewModel)
                }
            }
        }
    }
}

@Composable
private fun GlassBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    backdrop: com.kyant.backdrop.Backdrop
) {
    val accent = accentColor()
    val isLight = !isSystemInDarkTheme()

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 22.dp, top = 8.dp)
            .glassBottomBar(backdrop = backdrop)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (selected) accent else Color.Gray,
                    animationSpec = tween(220),
                    label = "tab-label-color-$index"
                )
                val labelScale by animateFloatAsState(
                    targetValue = if (selected) 1.04f else 1f,
                    animationSpec = tween(220),
                    label = "tab-label-scale-$index"
                )
                val indicatorPadding by animateDpAsState(
                    targetValue = if (selected) 0.dp else 8.dp,
                    animationSpec = tween(220),
                    label = "tab-indicator-padding-$index"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .padding(horizontal = indicatorPadding)
                                .glassTabIndicator(backdrop = backdrop)
                        )
                    }
                    Text(
                        label,
                        color = labelColor,
                        modifier = Modifier.graphicsLayer {
                            scaleX = labelScale
                            scaleY = labelScale
                        }
                    )
                }
            }
        }
    }
}
