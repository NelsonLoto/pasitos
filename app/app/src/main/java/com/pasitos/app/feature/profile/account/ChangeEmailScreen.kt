package com.pasitos.app.feature.profile.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun ChangeEmailScreen(
    onBackClick: () -> Unit,
    onCreatePasswordClick: () -> Unit,
    viewModel: ChangeEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChangeEmailContent(
        state = state,
        onNewEmailChange = viewModel::onNewEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSend = viewModel::onSend,
        onCreatePasswordClick = onCreatePasswordClick,
        onBackClick = onBackClick,
    )
}

@Composable
private fun ChangeEmailContent(
    state: ChangeEmailUiState,
    onNewEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSend: () -> Unit,
    onCreatePasswordClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountFormScaffold(title = "Email", isOnline = state.isOnline, error = state.error, onBackClick = onBackClick) {
        when {
            state.sentTo != null -> {
                Text(text = "Revisá tu email", style = PsType.H3)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Te mandamos un link a ${state.sentTo} para confirmar el cambio. Hasta que lo abras, " +
                        "seguís usando ${state.currentEmail}. Vamos a avisarle del cambio a tu email anterior.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(text = "Volver al perfil", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
            }

            // RF-CTA-11, CA4: para pedir el cambio hay que confirmar con la contraseña actual
            !state.hasPassword -> {
                Text(
                    text = "Tu email es ${state.currentEmail}. Tu cuenta usa Google y todavía no tiene contraseña: " +
                        "para cambiar el email, primero creá una.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(text = "Crear una contraseña", onClick = onCreatePasswordClick, modifier = Modifier.fillMaxWidth())
            }

            else -> {
                Text(
                    text = "Tu email actual es ${state.currentEmail}. Te vamos a mandar un link al email nuevo para " +
                        "confirmarlo; hasta entonces seguís usando el actual.",
                    style = PsType.BodySmall,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(20.dp))
                PsInputField(
                    label = "Email nuevo",
                    value = state.newEmail,
                    onValueChange = onNewEmailChange,
                    errorText = state.newEmailError,
                    keyboardType = KeyboardType.Email,
                    enabled = !state.isSending,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PsInputField(
                    label = "Contraseña actual",
                    value = state.password,
                    onValueChange = onPasswordChange,
                    errorText = state.passwordError,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isSending,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(
                    text = "Enviar link de confirmación",
                    onClick = onSend,
                    variant = PsButtonVariant.Primary,
                    enabled = state.canSend,
                    isLoading = state.isSending,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChangeEmailPreview() {
    PsTheme {
        ChangeEmailContent(
            state = ChangeEmailUiState(currentEmail = "maria@email.com"),
            onNewEmailChange = {},
            onPasswordChange = {},
            onSend = {},
            onCreatePasswordClick = {},
            onBackClick = {},
        )
    }
}
