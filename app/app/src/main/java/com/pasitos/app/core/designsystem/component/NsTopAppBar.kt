package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.R
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

/** Equivalente al componente "Top App Bar" (1:54), variantes Back=True/False. */
@Composable
fun NsTopAppBar(
    title: String,
    showBack: Boolean,
    onBackClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (showBack) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
        }
        Text(text = title, style = NsType.H3)
    }
}

@Preview(showBackground = true)
@Composable
private fun NsTopAppBarPreview() {
    NsTheme {
        NsTopAppBar(title = "Crear cuenta", showBack = true)
    }
}
