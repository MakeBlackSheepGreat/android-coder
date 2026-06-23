package com.dlzz.coder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop

val accentColor = Color(0xFF0091FF)
val containerLight = Color(0xFFFAFAFA).copy(0.4f)
val containerDark = Color(0xFF121212).copy(0.4f)

private val lightScheme = lightColorScheme(
    primary = accentColor,
    surface = Color(0xFFF5F5F7),
    background = Color(0xFFE8E8ED),
    onSurface = Color(0xFF1C1C1E)
)
private val darkScheme = darkColorScheme(
    primary = accentColor,
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF000000),
    onSurface = Color(0xFFE5E5EA)
)

@Composable
fun rememberGlassBackdrop(): Backdrop {
    return rememberCanvasBackdrop {}
}

@Composable
fun rememberLayerBackdrop(): Backdrop {
    return kyantRememberLayerBackdrop()
}

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) darkScheme else lightScheme
    MaterialTheme(colorScheme = colors, content = content)
}
