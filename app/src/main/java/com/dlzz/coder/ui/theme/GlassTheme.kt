package com.dlzz.coder.ui.theme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

val accentLight = Color(0xFF0088FF)
val accentDark = Color(0xFF0091FF)

val containerLight = Color(0xFFFAFAFA).copy(0.4f)
val containerDark = Color(0xFF121212).copy(0.4f)

val cardSurfaceLight = Color.White.copy(0.5f)
val cardSurfaceDark = Color.White.copy(0.06f)

@Composable
fun accentColor(): Color = if (isSystemInDarkTheme()) accentDark else accentLight

@Composable
fun containerColor(): Color = if (isSystemInDarkTheme()) containerDark else containerLight

@Composable
fun cardSurfaceColor(): Color = if (isSystemInDarkTheme()) cardSurfaceDark else cardSurfaceLight

private val lightScheme = lightColorScheme(
    primary = accentLight,
    surface = Color(0xFFF5F5F7),
    background = Color(0xFFE8E8ED),
    onSurface = Color(0xFF1C1C1E)
)
private val darkScheme = darkColorScheme(
    primary = accentDark,
    surface = Color(0xFF1C1C1E),
    background = Color(0xFF000000),
    onSurface = Color(0xFFE5E5EA)
)

private val appTypography = Typography().let { base ->
    val platformStyle = PlatformTextStyle(includeFontPadding = true)
    base.copy(
        headlineMedium = base.headlineMedium.copy(platformStyle = platformStyle, lineHeight = base.headlineMedium.fontSize * 1.22f),
        titleMedium = base.titleMedium.copy(platformStyle = platformStyle, lineHeight = base.titleMedium.fontSize * 1.28f),
        titleSmall = base.titleSmall.copy(platformStyle = platformStyle, lineHeight = base.titleSmall.fontSize * 1.28f),
        bodyMedium = base.bodyMedium.copy(platformStyle = platformStyle, lineHeight = base.bodyMedium.fontSize * 1.38f),
        bodySmall = base.bodySmall.copy(platformStyle = platformStyle, lineHeight = base.bodySmall.fontSize * 1.34f),
        labelLarge = base.labelLarge.copy(platformStyle = platformStyle, lineHeight = base.labelLarge.fontSize * 1.24f),
        labelSmall = base.labelSmall.copy(platformStyle = platformStyle, lineHeight = base.labelSmall.fontSize * 1.22f)
    )
}

@Composable
fun rememberGlassBackdrop(): Backdrop = rememberCanvasBackdrop {}

@Composable
fun rememberLayerBackdrop(): Backdrop = kyantRememberLayerBackdrop()

@Composable
fun Modifier.glassClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    onClick = onClick
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.glassCombinedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = combinedClickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    onClick = onClick,
    onLongClick = onLongClick
)

@Composable
fun Modifier.glassCard(
    backdrop: Backdrop = rememberCanvasBackdrop {},
    cornerRadius: Dp = 16.dp,
    blur: Dp = 4.dp,
    lensNear: Dp = 12.dp,
    lensFar: Dp = 24.dp,
    surfaceColor: Color = cardSurfaceColor()
): Modifier {
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(cornerRadius) },
        effects = {
            vibrancy()
            blur(blur.toPx())
            lens(lensNear.toPx(), lensFar.toPx(), depthEffect = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        innerShadow = { InnerShadow(radius = 2.dp) },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

@Composable
fun Modifier.glassBubble(
    backdrop: Backdrop = rememberCanvasBackdrop {},
    cornerRadius: Dp = 12.dp,
    surfaceColor: Color
): Modifier {
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(cornerRadius) },
        effects = {
            vibrancy()
            blur(2.dp.toPx())
            lens(8.dp.toPx(), 16.dp.toPx(), depthEffect = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        innerShadow = { InnerShadow(radius = 1.dp) },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

@Composable
fun Modifier.glassCapsule(
    backdrop: Backdrop = rememberCanvasBackdrop {},
    surfaceColor: Color = cardSurfaceColor()
): Modifier {
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
            vibrancy()
            blur(2.dp.toPx())
            lens(12.dp.toPx(), 24.dp.toPx(), depthEffect = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        innerShadow = { InnerShadow(radius = 1.dp) },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

@Composable
fun Modifier.glassDialog(
    backdrop: Backdrop = rememberCanvasBackdrop {},
    cornerRadius: Dp = 28.dp
): Modifier {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) containerDark.copy(1.4f) else containerLight.copy(1.5f)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { RoundedRectangle(cornerRadius) },
        effects = {
            colorControls(
                brightness = if (isDark) 0f else 0.2f,
                saturation = 1.5f
            )
            blur(if (isDark) 8.dp.toPx() else 16.dp.toPx())
            lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

@Composable
fun Modifier.glassBottomBar(
    backdrop: Backdrop
): Modifier {
    val surfaceColor = containerColor()
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
            vibrancy()
            blur(10.dp.toPx())
            lens(24.dp.toPx(), 24.dp.toPx(), depthEffect = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        innerShadow = { InnerShadow(radius = 2.dp) },
        onDrawSurface = { drawRect(surfaceColor) }
    )
}

@Composable
fun Modifier.glassTabIndicator(
    backdrop: Backdrop
): Modifier {
    val accent = accentColor()
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
            lens(10.dp.toPx(), 14.dp.toPx(), chromaticAberration = true)
        },
        highlight = { Highlight(style = HighlightStyle.Default()) },
        innerShadow = { InnerShadow(radius = 4.dp) },
        onDrawSurface = { drawRect(accent.copy(alpha = 0.15f)) }
    )
}

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) darkScheme else lightScheme
    MaterialTheme(colorScheme = colors, typography = appTypography, content = content)
}
