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
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

/** Fila de dato + badge, usada en historiales (RF04/RF06) y calendario de vacunación (RF10). */
@Composable
fun NsListRow(
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
        Text(text = label, style = NsType.BodyRegular)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = NsType.BodyMedium, color = NsColor.TextSecondary)
            trailing?.let {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                it()
            }
        }
    }
    if (showDivider) {
        HorizontalDivider(color = NsColor.Border)
    }
}

@Preview(showBackground = true)
@Composable
private fun NsListRowPreview() {
    NsTheme {
        NsListRow(
            label = "15/07/2026",
            value = "12.4 kg",
            trailing = { NsBadge(text = "P50", tone = NsBadgeTone.Success) },
            modifier = Modifier.padding(16.dp),
        )
    }
}
