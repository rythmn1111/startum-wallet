package com.nfcwallet.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Brand colours ────────────────────────────────────────────────────────────

val PurpleAccent  = Color(0xFFA855F7)   // Primary accent – matching iOS
val DarkBackground = Color(0xFF0F0C29)  // Deep navy / dark bg
val DarkSurface   = Color(0xFF1A1740)   // Slightly lighter surface
val DarkCard      = Color(0xFF221E4A)   // Card / container background
val OnDark        = Color(0xFFFFFFFF)   // Primary text on dark bg
val OnDarkMuted   = Color(0xFFB0A8D0)   // Secondary / muted text
val ErrorRed      = Color(0xFFEF4444)
val SuccessGreen  = Color(0xFF22C55E)

// ─── Dark colour scheme ───────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = PurpleAccent,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFF6D28D9),
    onPrimaryContainer = Color.White,
    secondary          = Color(0xFF7C3AED),
    onSecondary        = Color.White,
    background         = DarkBackground,
    onBackground       = OnDark,
    surface            = DarkSurface,
    onSurface          = OnDark,
    surfaceVariant     = DarkCard,
    onSurfaceVariant   = OnDarkMuted,
    error              = ErrorRed,
    onError            = Color.White,
    outline            = Color(0xFF4C1D95),
    inverseSurface     = Color(0xFFF3F0FF),
    inverseOnSurface   = DarkBackground,
    inversePrimary     = Color(0xFF6D28D9),
)

// ─── Light fallback (not primary use-case but keeps Material3 happy) ─────────

private val LightColorScheme = lightColorScheme(
    primary            = PurpleAccent,
    onPrimary          = Color.White,
    secondary          = Color(0xFF7C3AED),
    background         = Color(0xFFF3F0FF),
    surface            = Color.White,
    onBackground       = Color(0xFF0F0C29),
    onSurface          = Color(0xFF0F0C29),
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun NFCWalletTheme(
    darkTheme: Boolean = true,   // Always dark for this app
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MaterialTheme.typography,
        content     = content
    )
}
