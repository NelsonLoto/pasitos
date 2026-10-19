package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.app.ui.theme.extendedColors

/** [Neutral] es para datos informativos, por ejemplo una vacuna programada (RN-VAC-01). */
enum class PsBadgeTone { Success, Warning, Error, Info, Neutral }

/** Etiqueta de estado. Con [icon], el estado se lee también sin distinguir colores (RF-VAC-01, CA1). */
@Composable
fun PsBadge(text: String, tone: PsBadgeTone, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val color = when (tone) {
        PsBadgeTone.Success -> MaterialTheme.extendedColors.success
        PsBadgeTone.Warning -> MaterialTheme.extendedColors.warning
        PsBadgeTone.Error -> MaterialTheme.colorScheme.error
        PsBadgeTone.Info -> MaterialTheme.colorScheme.primary
        PsBadgeTone.Neutral -> PsColor.TextSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = color, modifier = Modifier.padding(end = 4.dp).size(14.dp))
        }
        Text(text = text, style = PsType.Caption, color = color)
    }
}

@Preview(showBackground = true)
@Composable
private fun PsBadgePreview() {
    PsTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            PsBadge(text = "Aplicada", tone = PsBadgeTone.Success)
            PsBadge(text = "Pendiente", tone = PsBadgeTone.Warning, modifier = Modifier.padding(start = 8.dp))
            PsBadge(text = "Atrasada", tone = PsBadgeTone.Error, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
