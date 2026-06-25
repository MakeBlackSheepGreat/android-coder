package com.dlzz.coder.ui.components

import android.widget.TextView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val markwon = remember(isDark) {
        val theme: Prism4jTheme = if (isDark) {
            Prism4jThemeDarkula.create()
        } else {
            Prism4jThemeDefault.create()
        }

        Markwon.builder(context)
            .usePlugin(SyntaxHighlightPlugin.create(theme))
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(color.toArgb())
                textSize = style.fontSize.value
            }
        },
        update = { textView ->
            textView.setTextColor(color.toArgb())
            markwon.setMarkdown(textView, markdown)
        }
    )
}
