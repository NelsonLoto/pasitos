package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Fila de menú (usada en Perfil/Configuración): ícono + label + chevron. */
@Composable
fun NsMenuRow(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = NsColor.TextSecondary)
            Text(text = label, style = NsType.BodyRegular, modifier = Modifier.padding(start = 12.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = NsColor.TextSecondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun NsMenuRowPreview() {
    NsTheme {
        NsMenuRow(
            icon = Icons.Filled.Notifications,
            label = "Notificaciones",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
