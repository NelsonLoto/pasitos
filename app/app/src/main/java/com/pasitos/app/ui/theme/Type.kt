package com.pasitos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pasitos.app.R

val BalooFamily = FontFamily(
    Font(R.font.baloo2, FontWeight.SemiBold),
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
)

/** Los 8 estilos de texto del Design System de Figma. */
object PsType {
    val H1 = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp)
    val H2 = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp)
    val H3 = TextStyle(fontFamily = BalooFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp)
    val BodyRegular = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val BodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val BodySmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Caption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val ButtonLabel = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
}

// Nota: tamaños en sp son un punto de partida razonable — confirmar contra las specs
// exactas de Figma (estilos de texto) cuando se haga la revisión visual pantalla por pantalla.
val PsMaterialTypography = Typography(
    headlineLarge = PsType.H1,
    headlineMedium = PsType.H2,
    headlineSmall = PsType.H3,
    bodyLarge = PsType.BodyRegular,
    bodyMedium = PsType.BodyMedium,
    bodySmall = PsType.BodySmall,
    labelSmall = PsType.Caption,
    labelLarge = PsType.ButtonLabel,
)
