package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Contenedor con el efecto "elevation/card" (drop shadow) del Design System. */
@Composable
fun NsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun NsCardPreview() {
    NsTheme {
        NsCard(modifier = Modifier.padding(16.dp)) {
            Text("Próxima vacuna", style = NsType.BodyMedium)
        }
    }
}
