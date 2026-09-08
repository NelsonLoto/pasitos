package com.ninosano.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

/**
 * Equivalente al componente "Input Field" (1:34) del Design System de Figma: label + input box.
 */
@Composable
fun NsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = NsType.BodySmall) },
            placeholder = placeholder?.let { { Text(it, style = NsType.BodyRegular, color = NsColor.TextSecondary) } },
            isError = errorText != null,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = NsColor.Border,
                errorBorderColor = MaterialTheme.colorScheme.error,
            ),
            textStyle = NsType.BodyRegular,
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = NsType.Caption,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NsInputFieldPreview() {
    NsTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            NsInputField(label = "Email", value = "familia@ejemplo.com", onValueChange = {})
            NsInputField(
                label = "Contraseña",
                value = "1234",
                onValueChange = {},
                errorText = "Mínimo 6 caracteres",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
