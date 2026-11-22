package com.aistudio.tdbassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.data.preferences.AccentPalette

private val TdbLightEmerald = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = ChampagneGold,
    onSecondary = ChampagneOnSecondary,
    secondaryContainer = ChampagneContainer,
    onSecondaryContainer = ChampagneOnContainer,
    tertiary = SageTertiary,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = SageOnContainer,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedInk,
    outline = LightOutline,
    error = ErrorCrimson
)

private val TdbLightChampagne = TdbLightEmerald.copy(
    primary = Color(0xFF936A21),
    primaryContainer = ChampagneContainer,
    onPrimaryContainer = ChampagneOnContainer,
    secondary = EmeraldPrimary,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldOnContainer
)

private val TdbLightTeal = TdbLightEmerald.copy(
    primary = Color(0xFF0E6B6B),
    primaryContainer = Color(0xFFD2F0EF),
    onPrimaryContainer = Color(0xFF063A3A),
    secondary = ChampagneGold
)

private val TdbDarkEmerald = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkEmeraldOnContainer,
    secondary = DarkChampagne,
    onSecondary = DarkBackground,
    secondaryContainer = DarkChampagneContainer,
    onSecondaryContainer = DarkChampagneOnContainer,
    tertiary = DarkEmeraldPrimary,
    tertiaryContainer = DarkEmeraldContainer,
    onTertiaryContainer = DarkEmeraldOnContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    error = DarkErrorCrimson
)

private val TdbDarkChampagne = TdbDarkEmerald.copy(
    primary = DarkChampagne,
    onPrimary = DarkBackground,
    primaryContainer = DarkChampagneContainer,
    onPrimaryContainer = DarkChampagneOnContainer,
    secondary = DarkEmeraldPrimary,
    secondaryContainer = DarkEmeraldContainer,
    onSecondaryContainer = DarkEmeraldOnContainer
)

private val TdbDarkTeal = TdbDarkEmerald.copy(
    primary = Color(0xFF6FE3E0),
    onPrimary = Color(0xFF033737),
    primaryContainer = Color(0xFF0D4E4D),
    onPrimaryContainer = Color(0xFFD2F0EF)
)

val TdbShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun TdbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentPalette: AccentPalette = AccentPalette.EMERALD,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && accentPalette == AccentPalette.CHAMPAGNE -> TdbDarkChampagne
        darkTheme && accentPalette == AccentPalette.ROYAL_TEAL -> TdbDarkTeal
        darkTheme -> TdbDarkEmerald
        accentPalette == AccentPalette.CHAMPAGNE -> TdbLightChampagne
        accentPalette == AccentPalette.ROYAL_TEAL -> TdbLightTeal
        else -> TdbLightEmerald
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TdbTypography,
        shapes = TdbShapes,
        content = content
    )
}
