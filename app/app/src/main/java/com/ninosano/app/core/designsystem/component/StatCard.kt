package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Tarjeta de estado del dashboard Home (próxima vacuna, último peso, etc.). */
@Composable
fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    NsCard(modifier = modifier) {
        Row {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = label, style = NsType.Caption, color = NsColor.TextSecondary)
                Text(text = value, style = NsType.H3)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatCardPreview() {
    NsTheme {
        StatCard(
            icon = Icons.Filled.Scale,
            label = "Último peso",
            value = "12.4 kg",
            modifier = Modifier.padding(16.dp),
        )
    }
}
