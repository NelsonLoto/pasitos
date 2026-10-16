package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** Fila de dato + badge, usada en historiales (RF04/RF06) y calendario de vacunación (RF10). */
@Composable
fun PsListRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(text = label, style = PsType.BodyRegular)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = PsType.BodyMedium, color = PsColor.TextSecondary)
            trailing?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                it()
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(color = PsColor.Border)
    }
}

@Preview(showBackground = true)
@Composable
private fun PsListRowPreview() {
    PsTheme {
        PsListRow(
            label = "15/07/2026",
            value = "12.4 kg",
            trailing = { PsBadge(text = "P50", tone = PsBadgeTone.Success) },
            modifier = Modifier.padding(16.dp),
        )
    }
}
