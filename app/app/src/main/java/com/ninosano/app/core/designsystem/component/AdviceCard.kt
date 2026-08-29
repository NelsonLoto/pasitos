package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Tarjeta de "Consejos por edad" (RF14): tinte + link "Leer más". */
@Composable
fun AdviceCard(title: String, summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    NsCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Text(text = title, style = NsType.H3)
            Text(
                text = summary,
                style = NsType.BodySmall,
                color = NsColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Leer más",
                style = NsType.BodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdviceCardPreview() {
    NsTheme {
        AdviceCard(
            title = "¿Qué se viene? 6 meses",
            summary = "Comienza la introducción de alimentos sólidos.",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
