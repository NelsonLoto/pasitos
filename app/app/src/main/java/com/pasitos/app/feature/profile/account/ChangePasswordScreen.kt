package com.pasitos.app.feature.profile.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun ChangePasswordScreen(
    onBackClick: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChangePasswordContent(
        state = state,
        onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onChange = viewModel::onChange,
        onSendCreationEmail = viewModel::onSendCreationEmail,
        onBackClick = onBackClick,
    )
}

@Composable
private fun ChangePasswordContent(
    state: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChange: () -> Unit,
    onSendCreationEmail: () -> Unit,
    onBackClick: () -> Unit,
) {
    val title = if (state.hasPassword) "Contraseña" else "Crear una contraseña"
    AccountFormScaffold(title = title, isOnline = state.isOnline, error = state.error, onBackClick = onBackClick) {
        when {
            state.result == PasswordResult.CHANGED -> Done(
                title = "Contraseña actualizada",
                text = "Cerramos tu sesión en los demás dispositivos. En este seguís con la sesión abierta.",
                onBackClick = onBackClick,
            )

            state.result == PasswordResult.CREATION_EMAIL_SENT -> Done(
                title = "Revisá tu email",
                text = "Te mandamos un link a ${state.email} para crear tu contraseña. Al crearla se cierran tus " +
                    "sesiones abiertas, también esta: después ingresá con tu email y la contraseña nueva, o con Google.",
                onBackClick = onBackClick,
            )

            // RF-CTA-11, CA4: sin contraseña no hay una actual para confirmar; el email la reemplaza
            !state.hasPassword -> {
                Text(
                    text = "Tu cuenta usa Google y todavía no tiene contraseña. Te mandamos un link a ${state.email} " +
                        "para crearla; así también vas a poder ingresar con tu email.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(
                    text = "Mandarme el link",
                    onClick = onSendCreationEmail,
                    enabled = state.canSave,
                    isLoading = state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                PsInputField(
                    label = "Contraseña actual",
                    value = state.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    errorText = state.currentPasswordError,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isSaving,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PsInputField(
                    label = "Contraseña nueva",
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    errorText = state.newPasswordError,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isSaving,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PsInputField(
                    label = "Repetí la contraseña nueva",
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    errorText = state.confirmPasswordError,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isSaving,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Al menos 8 caracteres, con letras y números. Se cierra tu sesión en los demás dispositivos.",
                    style = PsType.Caption,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(
                    text = "Cambiar contraseña",
                    onClick = onChange,
                    enabled = state.canSave,
                    isLoading = state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Done(title: String, text: String, onBackClick: () -> Unit) {
    Text(text = title, style = PsType.H3)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = text, style = PsType.BodyRegular, color = PsColor.TextSecondary)
    Spacer(modifier = Modifier.height(24.dp))
    PsButton(text = "Volver al perfil", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true, name = "Cambiar contraseña")
@Composable
private fun ChangePasswordPreview() {
    PsTheme {
        ChangePasswordContent(
            state = ChangePasswordUiState(email = "maria@email.com"),
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onChange = {},
            onSendCreationEmail = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Crear una contraseña")
@Composable
private fun CreatePasswordPreview() {
    PsTheme {
        ChangePasswordContent(
            state = ChangePasswordUiState(email = "maria@gmail.com", hasPassword = false),
            onCurrentPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onChange = {},
            onSendCreationEmail = {},
            onBackClick = {},
        )
    }
}
