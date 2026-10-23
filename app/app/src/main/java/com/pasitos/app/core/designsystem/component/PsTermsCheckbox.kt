package com.pasitos.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsType

/**
 * Aceptación expresa de los términos y la política de privacidad (RN-CTA-02): la casilla nunca
 * aparece marcada de entrada. La usan el registro con email y el primer ingreso con Google.
 */
@Composable
fun PsTermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        ) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
            Text(
                text = "Acepto los Términos y Condiciones y la Política de Privacidad",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        errorText?.let { error ->
            Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
