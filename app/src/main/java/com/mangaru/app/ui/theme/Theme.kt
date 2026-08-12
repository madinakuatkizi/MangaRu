package com.mangaru.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = CardBackground,
    primary = PrimaryAccent,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MangaRuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
