package com.ninosano.app.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/** Equivalente al componente "Top App Bar" (1:54), variantes Back=True/False. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NsTopAppBar(
    title: String,
    showBack: Boolean,
    onBackClick: () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, style = NsType.H3) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun NsTopAppBarPreview() {
    NsTheme {
        NsTopAppBar(title = "Curva de crecimiento", showBack = true)
    }
}
