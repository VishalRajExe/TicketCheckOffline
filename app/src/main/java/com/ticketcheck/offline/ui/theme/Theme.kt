package com.ticketcheck.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2150),
    onPrimaryContainer = Color(0xFFDDD3FF),
    secondary = Success,
    onSecondary = Color(0xFF06281B),
    secondaryContainer = Color(0xFF0E3A2B),
    onSecondaryContainer = Color(0xFFB8F5DE),
    tertiary = WarningAmber,
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFF3F2E05),
    onTertiaryContainer = Color(0xFFFDE9B8),
    background = BgDeep,
    onBackground = OnDark,
    surface = BgElevated,
    onSurface = OnDark,
    surfaceVariant = SurfaceGlass,
    onSurfaceVariant = OnDarkMuted,
    surfaceTint = Primary,
    inverseSurface = Color(0xFFE6E9F2),
    inverseOnSurface = BgDeep,
    inversePrimary = Color(0xFF4C34A8),
    outline = Color(0xFF39415A),
    outlineVariant = Color(0xFF232B40),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3B1113),
    onErrorContainer = Color(0xFFFDD8D8),
    scrim = Color(0xFF000000)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D4FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = Color(0xFF2A2150),
    secondary = Color(0xFF0E9F6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F5E7),
    onSecondaryContainer = Color(0xFF06402C),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCEDCB),
    onTertiaryContainer = Color(0xFF3F2E05),
    background = BgLight,
    onBackground = OnLight,
    surface = BgElevatedLight,
    onSurface = OnLight,
    surfaceVariant = Color(0xFFE9EDF6),
    onSurfaceVariant = OnLightMuted,
    surfaceTint = Color(0xFF6D4FE0),
    inverseSurface = Color(0xFF2A3042),
    inverseOnSurface = Color(0xFFF4F6FB),
    inversePrimary = Primary,
    outline = Color(0xFF8B93A8),
    outlineVariant = Color(0xFFD5DAE6),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFDD8D8),
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0xFF000000)
)

@Composable
fun TicketCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
