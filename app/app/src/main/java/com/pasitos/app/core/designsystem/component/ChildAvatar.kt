package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrueltyFree
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.api.child.AVATAR_KEYS

/** Dibujo de un avatar predefinido: un ícono de Material sobre un fondo pastel de la paleta. */
private data class AvatarArt(val icon: ImageVector, val tone: Color, val label: String)

/** Un dibujo por cada clave de `AVATAR_KEYS` (RN-HIJ-11). La clave no cambia aunque cambie el dibujo. */
private val avatarArt: Map<String, AvatarArt> = mapOf(
    "rabbit" to AvatarArt(Icons.Filled.CrueltyFree, PsColor.Accent, "Conejo"),
    "bird" to AvatarArt(Icons.Filled.FlutterDash, PsColor.Primary, "Pájaro"),
    "bee" to AvatarArt(Icons.Filled.EmojiNature, PsColor.Warning, "Abeja"),
    "paw" to AvatarArt(Icons.Filled.Pets, PsColor.Success, "Huellita"),
    "flower" to AvatarArt(Icons.Filled.LocalFlorist, PsColor.Accent, "Flor"),
    "tree" to AvatarArt(Icons.Filled.Park, PsColor.Success, "Árbol"),
    "sun" to AvatarArt(Icons.Filled.WbSunny, PsColor.Warning, "Sol"),
    "star" to AvatarArt(Icons.Filled.Star, PsColor.Primary, "Estrella"),
)

/** Nombre del avatar para lectores de pantalla ("Conejo"), o nulo si la clave no existe. */
fun avatarLabel(avatarKey: String): String? = avatarArt[avatarKey]?.label

/** Color determinístico por hijo para la inicial (misma paleta que usa Figma: primary/accent alternados). */
fun childAvatarColor(childId: String): Color =
    if (Math.floorMod(childId.hashCode(), 2) == 0) PsColor.Primary else PsColor.Accent

/**
 * Avatar de un hijo: su dibujo predefinido o, sin avatar o con una clave que esta versión de la
 * app no conoce, la inicial sobre [color].
 */
@Composable
fun ChildAvatar(avatarKey: String?, initial: String, color: Color, size: Dp = 46.dp) {
    val art = avatarKey?.let(avatarArt::get)
    if (art == null) {
        ChildAvatar(initial = initial, color = color, size = size)
        return
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(color = art.tone.copy(alpha = 0.3f).compositeOver(PsColor.Surface), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = art.icon, contentDescription = null, tint = PsColor.TextPrimary, modifier = Modifier.size(size * 0.55f))
    }
}

/** La inicial sobre un círculo de color. También la usa Perfil para la cuenta. */
@Composable
fun ChildAvatar(initial: String, color: Color, size: Dp = 46.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = PsType.H3, color = PsColor.TextOnPrimary)
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildAvatarPreview() {
    PsTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            (AVATAR_KEYS + listOf<String?>(null)).forEach { key ->
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    ChildAvatar(avatarKey = key, initial = "L", color = PsColor.Primary, size = 40.dp)
                }
            }
        }
    }
}
