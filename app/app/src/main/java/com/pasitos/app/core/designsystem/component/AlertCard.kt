package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType
import com.pasitos.app.ui.theme.extendedColors

enum class NsAlertTone { Info, Warning, Error }

/** Tarjeta con borde de estado, usada para RF09 (alerta de rango fuera de percentil). */
@Composable
fun AlertCard(text: String, tone: NsAlertTone, modifier: Modifier = Modifier) {
    val color = when (tone) {
        NsAlertTone.Info -> MaterialTheme.colorScheme.primary
        NsAlertTone.Warning -> MaterialTheme.extendedColors.warning
        NsAlertTone.Error -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .background(color = color.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = color, shape = RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(4.dp)
                .background(color = color, shape = RoundedCornerShape(2.dp)),
        )
        Text(text = text, style = NsType.BodySmall, modifier = Modifier.padding(start = 12.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun AlertCardPreview() {
    NsTheme {
        AlertCard(
            text = "Esta medición está fuera del rango normal (percentil 3-97). Se recomienda consultar al pediatra.",
            tone = NsAlertTone.Warning,
            modifier = Modifier.padding(16.dp),
        )
    }
}
