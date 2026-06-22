package com.dlzz.coder.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dlzz.coder.viewmodel.BridgeViewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

enum class MainTab(val label: String) { SESSIONS("Sessions"), FILES("Files"), SETTINGS("Settings") }

@Composable
fun MainScreen(
    bridgeViewModel: BridgeViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToFilePreview: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = MainTab.entries
    val backdrop = rememberLayerBackdrop()

    Scaffold(
        bottomBar = {
            GlassBottomBar(
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                tabs = tabs.map { it.label },
                backdrop = backdrop
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tabs[selectedTab]) {
                MainTab.SESSIONS -> PlaceholderScreen("Sessions")
                MainTab.FILES -> PlaceholderScreen("Files")
                MainTab.SETTINGS -> PlaceholderScreen("Settings")
            }
        }
    }
}

@Composable
private fun GlassBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    backdrop: Backdrop
) {
    val isLight = !isSystemInDarkTheme()
    val accent = Color(0xFF0091FF)
    val container = if (isLight) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(24f.dp.toPx(), 24f.dp.toPx())
                },
                onDrawSurface = { drawRect(container) }
            )
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .then(
                            if (index == selectedIndex) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { Capsule() },
                                    effects = {
                                        lens(10f.dp.toPx(), 14f.dp.toPx())
                                    },
                                    highlight = { Highlight(style = HighlightStyle.Default()) },
                                    shadow = { Shadow() },
                                    innerShadow = { InnerShadow(radius = 4f.dp) },
                                    onDrawSurface = {
                                        drawRect(accent.copy(alpha = 0.15f))
                                    }
                                )
                            } else Modifier
                        )
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (index == selectedIndex) accent else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
    }
}
