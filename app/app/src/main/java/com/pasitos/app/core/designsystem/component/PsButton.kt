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
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

enum class PsButtonVariant { Primary, Secondary }

@Composable
fun PsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PsButtonVariant = PsButtonVariant.Primary,
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
                Text(text = text, style = PsType.ButtonLabel)
            }
        }
    }
    when (variant) {
        PsButtonVariant.Primary -> Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = modifier.height(52.dp),
        ) { content() }

        PsButtonVariant.Secondary -> OutlinedButton(
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
private fun PsButtonPreview() {
    PsTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PsButton(text = "Iniciar sesión", onClick = {}, variant = PsButtonVariant.Primary)
            PsButton(text = "Cancelar", onClick = {}, variant = PsButtonVariant.Secondary)
            PsButton(text = "Cargando", onClick = {}, isLoading = true)
        }
    }
}
