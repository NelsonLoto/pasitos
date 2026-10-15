package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

enum class NsButtonVariant { Primary, Secondary }

@Composable
fun NsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NsButtonVariant = NsButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    val content: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                leadingIcon?.invoke()
                Text(text = text, style = NsType.ButtonLabel)
            }
        }
    }
    when (variant) {
        NsButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = modifier.height(52.dp),
        ) { content() }

        NsButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = modifier.height(52.dp),
        ) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun NsButtonPreview() {
    NsTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NsButton(text = "Iniciar sesión", onClick = {}, variant = NsButtonVariant.Primary)
            NsButton(text = "Cancelar", onClick = {}, variant = NsButtonVariant.Secondary)
            NsButton(text = "Cargando", onClick = {}, isLoading = true)
        }
    }
}
