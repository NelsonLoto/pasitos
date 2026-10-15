package com.pasitos.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Tokens de color que no tienen slot nativo en el ColorScheme de Material 3. */
data class NsExtendedColors(
    val success: Color,
    val warning: Color,
)

private val LocalNsExtendedColors = staticCompositionLocalOf {
    NsExtendedColors(success = NsColor.Success, warning = NsColor.Warning)
}

private val NsLightColorScheme = lightColorScheme(
    primary = NsColor.Primary,
    onPrimary = NsColor.TextOnPrimary,
    tertiary = NsColor.Accent,
    onTertiary = NsColor.TextOnPrimary,
    error = NsColor.Error,
    onError = NsColor.TextOnPrimary,
    background = NsColor.Background,
    onBackground = NsColor.TextPrimary,
    surface = NsColor.Surface,
    onSurface = NsColor.TextPrimary,
    onSurfaceVariant = NsColor.TextSecondary,
    outline = NsColor.Border,
)

/**
 * Tema "Pasitos". Solo define modo claro por ahora: el Design System de Figma
 * no tiene un modo oscuro definido todavía.
 */
@Composable
fun NsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNsExtendedColors provides NsExtendedColors(success = NsColor.Success, warning = NsColor.Warning),
    ) {
        MaterialTheme(
            colorScheme = NsLightColorScheme,
            typography = NsMaterialTypography,
            content = content,
        )
    }
}

val MaterialTheme.extendedColors: NsExtendedColors
    @Composable get() = LocalNsExtendedColors.current
