package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.app.ui.theme.extendedColors

enum class PsBadgeTone { Success, Warning, Error, Info }

@Composable
fun PsBadge(text: String, tone: PsBadgeTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        PsBadgeTone.Success -> MaterialTheme.extendedColors.success
        PsBadgeTone.Warning -> MaterialTheme.extendedColors.warning
        PsBadgeTone.Error -> MaterialTheme.colorScheme.error
        PsBadgeTone.Info -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
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
