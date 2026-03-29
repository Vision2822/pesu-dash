package com.pesu.pesudash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.R

object AccentPresets {
    val Blue   = Color(0xFF3B82F6)
    val Purple = Color(0xFFA855F7)
    val Green  = Color(0xFF22C55E)
    val Rose   = Color(0xFFF43F5E)
    val Orange = Color(0xFFF97316)
    val Cyan   = Color(0xFF06B6D4)
}

data class ShadcnColors(
    val background:   Color,
    val card:         Color,
    val cardHover:    Color,
    val popover:      Color,
    val border:       Color,
    val borderSubtle: Color,
    val input:        Color,
    val foreground:   Color,
    val mutedFg:      Color,
    val dimFg:        Color,
    val primary:      Color,
    val primaryFg:    Color,
    val green:        Color,
    val greenMuted:   Color,
    val red:          Color,
    val redMuted:     Color,
    val yellow:       Color,
    val yellowMuted:  Color,
    val orange:       Color,
    val orangeMuted:  Color,
    val blue:         Color,
    val ring:         Color,
    val accent:       Color
)

fun darkColors(accent: Color = AccentPresets.Blue) = ShadcnColors(
    background   = Color(0xFF09090B),
    card         = Color(0xFF0F0F12),
    cardHover    = Color(0xFF18181B),
    popover      = Color(0xFF0F0F12),
    border       = Color(0xFF27272A),
    borderSubtle = Color(0xFF1E1E22),
    input        = Color(0xFF27272A),
    foreground   = Color(0xFFFAFAFA),
    mutedFg      = Color(0xFFA1A1AA),
    dimFg        = Color(0xFF71717A),
    primary      = Color(0xFFFAFAFA),
    primaryFg    = Color(0xFF09090B),
    green        = Color(0xFF22C55E),
    greenMuted   = Color(0xFF22C55E).copy(alpha = 0.15f),
    red          = Color(0xFFEF4444),
    redMuted     = Color(0xFFEF4444).copy(alpha = 0.15f),
    yellow       = Color(0xFFEAB308),
    yellowMuted  = Color(0xFFEAB308).copy(alpha = 0.15f),
    orange       = Color(0xFFF97316),
    orangeMuted  = Color(0xFFF97316).copy(alpha = 0.15f),
    blue         = Color(0xFF3B82F6),
    ring         = Color(0xFFD4D4D8),
    accent       = accent
)

fun lightColors(accent: Color = AccentPresets.Blue) = ShadcnColors(
    background   = Color(0xFFFFFFFF),
    card         = Color(0xFFF9F9FB),
    cardHover    = Color(0xFFF1F1F5),
    popover      = Color(0xFFFFFFFF),
    border       = Color(0xFFE4E4E7),
    borderSubtle = Color(0xFFF0F0F2),
    input        = Color(0xFFE4E4E7),
    foreground   = Color(0xFF09090B),
    mutedFg      = Color(0xFF71717A),
    dimFg        = Color(0xFFA1A1AA),
    primary      = Color(0xFF09090B),
    primaryFg    = Color(0xFFFAFAFA),
    green        = Color(0xFF16A34A),
    greenMuted   = Color(0xFF16A34A).copy(alpha = 0.12f),
    red          = Color(0xFFDC2626),
    redMuted     = Color(0xFFDC2626).copy(alpha = 0.12f),
    yellow       = Color(0xFFCA8A04),
    yellowMuted  = Color(0xFFCA8A04).copy(alpha = 0.12f),
    orange       = Color(0xFFEA580C),
    orangeMuted  = Color(0xFFEA580C).copy(alpha = 0.12f),
    blue         = Color(0xFF2563EB),
    ring         = Color(0xFF52525B),
    accent       = accent
)

val LocalShadcnColors = staticCompositionLocalOf { darkColors() }

enum class ThemeMode { DARK, LIGHT, SYSTEM }

fun String.toThemeMode(): ThemeMode = when (this) {
    "LIGHT"  -> ThemeMode.LIGHT
    "DARK"   -> ThemeMode.DARK
    else     -> ThemeMode.SYSTEM
}

val Inter = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)

val ShadcnTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.3.sp
    ),
)

@Composable
fun PesuDashTheme(
    themeMode:   ThemeMode = ThemeMode.SYSTEM,
    accentColor: Color     = AccentPresets.Blue,
    content:     @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = if (isDark) darkColors(accentColor) else lightColors(accentColor)

    val m3Scheme = if (isDark) {
        darkColorScheme(
            primary          = colors.primary,
            onPrimary        = colors.primaryFg,
            background       = colors.background,
            onBackground     = colors.foreground,
            surface          = colors.card,
            onSurface        = colors.foreground,
            surfaceVariant   = colors.cardHover,
            onSurfaceVariant = colors.mutedFg,
            outline          = colors.border,
            outlineVariant   = colors.borderSubtle,
            error            = colors.red,
            onError          = Color.White
        )
    } else {
        lightColorScheme(
            primary          = colors.primary,
            onPrimary        = colors.primaryFg,
            background       = colors.background,
            onBackground     = colors.foreground,
            surface          = colors.card,
            onSurface        = colors.foreground,
            surfaceVariant   = colors.cardHover,
            onSurfaceVariant = colors.mutedFg,
            outline          = colors.border,
            outlineVariant   = colors.borderSubtle,
            error            = colors.red,
            onError          = Color.White
        )
    }

    CompositionLocalProvider(LocalShadcnColors provides colors) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography  = ShadcnTypography,
            content     = content
        )
    }
}

object AppTheme {
    val colors: ShadcnColors
        @Composable get() = LocalShadcnColors.current
}