package com.ninosano.app.core.designsystem.component

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
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
import com.ninosano.app.ui.theme.extendedColors

enum class NsBadgeTone { Success, Warning, Error, Info }

@Composable
fun NsBadge(text: String, tone: NsBadgeTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        NsBadgeTone.Success -> MaterialTheme.extendedColors.success
        NsBadgeTone.Warning -> MaterialTheme.extendedColors.warning
        NsBadgeTone.Error -> MaterialTheme.colorScheme.error
        NsBadgeTone.Info -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = NsType.Caption, color = color)
    }
}

@Preview(showBackground = true)
@Composable
private fun NsBadgePreview() {
    NsTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            NsBadge(text = "Aplicada", tone = NsBadgeTone.Success)
            NsBadge(text = "Pendiente", tone = NsBadgeTone.Warning, modifier = Modifier.padding(start = 8.dp))
            NsBadge(text = "Atrasada", tone = NsBadgeTone.Error, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
