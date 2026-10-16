package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** Tarjeta de "Consejos por edad" (RF14): tinte + link "Leer más". */
@Composable
fun AdviceCard(title: String, summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PsCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Text(text = title, style = PsType.H3)
            Text(
                text = summary,
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Leer más",
                style = PsType.BodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdviceCardPreview() {
    PsTheme {
        AdviceCard(
            title = "¿Qué se viene? 6 meses",
            summary = "Comienza la introducción de alimentos sólidos.",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
