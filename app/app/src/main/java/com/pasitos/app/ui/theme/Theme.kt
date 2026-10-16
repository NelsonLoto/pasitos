package com.pasitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tokens de color que no tienen slot nativo en el ColorScheme de Material 3. */
data class PsExtendedColors(
    val success: Color,
    val warning: Color,
)

private val LocalNsExtendedColors = staticCompositionLocalOf {
    PsExtendedColors(success = PsColor.Success, warning = PsColor.Warning)
}

private val PsLightColorScheme = lightColorScheme(
    primary = PsColor.Primary,
    onPrimary = PsColor.TextOnPrimary,
    tertiary = PsColor.Accent,
    onTertiary = PsColor.TextOnPrimary,
    error = PsColor.Error,
    onError = PsColor.TextOnPrimary,
    background = PsColor.Background,
    onBackground = PsColor.TextPrimary,
    surface = PsColor.Surface,
    onSurface = PsColor.TextPrimary,
    onSurfaceVariant = PsColor.TextSecondary,
    outline = PsColor.Border,
)

/**
 * Tema "Pasitos". Solo define modo claro por ahora: el Design System de Figma
 * no tiene un modo oscuro definido todavía.
 */
@Composable
fun PsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNsExtendedColors provides PsExtendedColors(success = PsColor.Success, warning = PsColor.Warning),
    ) {
        MaterialTheme(
            colorScheme = PsLightColorScheme,
            typography = PsMaterialTypography,
            content = content,
        )
    }
}

val MaterialTheme.extendedColors: PsExtendedColors
    @Composable get() = LocalNsExtendedColors.current
