package com.pasitos.app.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** "Olvidé mi contraseña" (RF-CTA-05, CA1). */
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordContent(
        state = state,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Recuperar contraseña", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            if (state.requestSent) {
                Text(
                    text = "Si el email está registrado, te enviamos un enlace para recuperar tu contraseña.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                PsButton(
                    text = "Volver a iniciar sesión",
                    onClick = onBackClick,
                    variant = PsButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            Text(
                text = "Ingresá el email de tu cuenta y te mandamos un enlace para definir una contraseña nueva.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Email",
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "tu@email.com",
                errorText = state.emailError,
                keyboardType = KeyboardType.Email,
                enabled = !state.isLoading,
            )

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Start)
            }

            Spacer(modifier = Modifier.height(20.dp))
            PsButton(
                text = "Enviar enlace",
                onClick = onSubmit,
                variant = PsButtonVariant.Primary,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    PsTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "familia@ejemplo.com"),
            onBackClick = {},
            onEmailChange = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, name = "Enviado")
@Composable
private fun ForgotPasswordScreenSentPreview() {
    PsTheme {
        ForgotPasswordContent(
            state = ForgotPasswordUiState(email = "familia@ejemplo.com", requestSent = true),
            onBackClick = {},
            onEmailChange = {},
            onSubmit = {},
        )
    }
}
