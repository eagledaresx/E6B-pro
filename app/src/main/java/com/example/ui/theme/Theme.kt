package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Immutable
data class CockpitSemanticColors(
    val safeGreen: Color,
    val warningAmber: Color,
    val dangerRed: Color,
    val vectorCyan: Color,
    val dialTick: Color,
    val gridLine: Color,
    val isNightRed: Boolean
)

val LocalCockpitColors = staticCompositionLocalOf {
    CockpitSemanticColors(
        safeGreen = InstrumentEmerald,
        warningAmber = InstrumentAmber,
        dangerRed = InstrumentCoral,
        vectorCyan = AvionicsCyan,
        dialTick = InstrumentTextSecondary,
        gridLine = CockpitOutline,
        isNightRed = false
    )
}

private val AvionicsDarkScheme = darkColorScheme(
    primary = AvionicsCyan,
    onPrimary = CockpitVoid,
    primaryContainer = CockpitPanelElevated,
    onPrimaryContainer = AvionicsCyan,
    secondary = InstrumentAmber,
    onSecondary = CockpitVoid,
    secondaryContainer = Color(0xFF2E2208),
    onSecondaryContainer = InstrumentAmber,
    tertiary = InstrumentEmerald,
    onTertiary = CockpitVoid,
    tertiaryContainer = Color(0xFF0A2E1E),
    onTertiaryContainer = InstrumentEmerald,
    background = CockpitSlate,
    onBackground = InstrumentTextPrimary,
    surface = CockpitPanel,
    onSurface = InstrumentTextPrimary,
    surfaceVariant = CockpitPanelElevated,
    onSurfaceVariant = InstrumentTextSecondary,
    outline = CockpitOutline,
    outlineVariant = Color(0xFF1D2F49),
    error = InstrumentCoral,
    onError = CockpitVoid,
    errorContainer = Color(0xFF3A1216),
    onErrorContainer = Color(0xFFFFB4AB)
)

private val NightRedScheme = darkColorScheme(
    primary = NightRedPrimary,
    onPrimary = NightRedVoid,
    primaryContainer = NightRedPanelElevated,
    onPrimaryContainer = NightRedTextPrimary,
    secondary = NightRedSecondary,
    onSecondary = NightRedVoid,
    secondaryContainer = Color(0xFF2D0A08),
    onSecondaryContainer = NightRedSecondary,
    tertiary = NightRedTertiary,
    onTertiary = NightRedTextPrimary,
    tertiaryContainer = Color(0xFF240404),
    onTertiaryContainer = NightRedTextPrimary,
    background = NightRedVoid,
    onBackground = NightRedTextPrimary,
    surface = NightRedSurface,
    onSurface = NightRedTextPrimary,
    surfaceVariant = NightRedPanel,
    onSurfaceVariant = NightRedTextSecondary,
    outline = NightRedOutline,
    outlineVariant = Color(0xFF330A0A),
    error = NightRedPrimary,
    onError = NightRedVoid,
    errorContainer = Color(0xFF420A08),
    onErrorContainer = NightRedTextPrimary
)

private val E6BShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun E6BProTheme(
    nightRedMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (nightRedMode) NightRedScheme else AvionicsDarkScheme
    val semanticColors = if (nightRedMode) {
        CockpitSemanticColors(
            safeGreen = NightRedSecondary,
            warningAmber = NightRedPrimary,
            dangerRed = NightRedPrimary,
            vectorCyan = NightRedPrimary,
            dialTick = NightRedTextSecondary,
            gridLine = NightRedOutline,
            isNightRed = true
        )
    } else {
        CockpitSemanticColors(
            safeGreen = InstrumentEmerald,
            warningAmber = InstrumentAmber,
            dangerRed = InstrumentCoral,
            vectorCyan = AvionicsCyan,
            dialTick = InstrumentTextSecondary,
            gridLine = CockpitOutline,
            isNightRed = false
        )
    }

    CompositionLocalProvider(LocalCockpitColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = E6BTypography,
            shapes = E6BShapes,
            content = content
        )
    }
}
