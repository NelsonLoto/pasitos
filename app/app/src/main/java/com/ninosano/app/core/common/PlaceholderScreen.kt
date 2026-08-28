package com.ninosano.app.core.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/**
 * Reutilizado por las pantallas todavía no implementadas, para que el grafo de
 * navegación de las 15 pantallas esté completo desde el día 1.
 */
@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title\n\nPróximamente",
            style = NsType.BodyMedium,
            color = NsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    NsTheme {
        PlaceholderScreen(title = "Home / Dashboard")
    }
}
