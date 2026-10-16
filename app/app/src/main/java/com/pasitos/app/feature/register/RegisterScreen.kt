package com.pasitos.app.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onRegisterSuccess() }
    }

    RegisterContent(
        state = state,
        onBackClick = onBackClick,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::onSubmit,
        onLoginClick = onLoginClick,
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onBackClick: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Crear cuenta", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = "Completá tus datos para empezar a registrar el crecimiento de tu hijo/a.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Nombre completo",
                value = state.fullName,
                onValueChange = onFullNameChange,
                placeholder = "Ej: María Gómez",
                errorText = state.fullNameError,
                enabled = !state.isLoading,
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

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Contraseña",
                value = state.password,
                onValueChange = onPasswordChange,
                errorText = state.passwordError,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Confirmar contraseña",
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                errorText = state.confirmPasswordError,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Al continuar aceptás los Términos y Condiciones y la Política de Privacidad.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
            )

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
            PsButton(
                text = "Crear cuenta",
                onClick = onSubmit,
                variant = PsButtonVariant.Primary,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLoginClick),
            ) {
                Text(text = "¿Ya tenés cuenta? ", style = PsType.BodySmall, color = PsColor.TextSecondary)
                Text(
                    text = "Iniciá sesión",
                    style = PsType.BodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    PsTheme {
        RegisterContent(
            state = RegisterUiState(),
            onBackClick = {},
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onLoginClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Con error")
@Composable
private fun RegisterScreenErrorPreview() {
    PsTheme {
        RegisterContent(
            state = RegisterUiState(
                fullName = "María Gómez",
                email = "demo@example.com",
                password = "123456",
                confirmPassword = "123456",
                generalError = "Ya existe una cuenta con este email",
            ),
            onBackClick = {},
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onLoginClick = {},
        )
    }
}
