package com.pasitos.app.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onLoginSuccess() }
    }

    if (state.emailVerificationPending) {
        EmailVerificationPendingContent(
            email = state.email,
            isResending = state.isResendingVerification,
            isChecking = state.isLoading,
            message = state.verificationMessage,
            onResendClick = viewModel::onResendVerificationClick,
            onAlreadyVerifiedClick = viewModel::onAlreadyVerifiedClick,
            onBackClick = viewModel::onBackFromVerificationClick,
        )
        return
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onGoogleSignInClick = viewModel::onGoogleSignInClick,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
    )
}

/** RF-CTA-02, CA1: pantalla "Verificá tu email", con reenviar y "Ya verifiqué". */
@Composable
private fun EmailVerificationPendingContent(
    email: String,
    isResending: Boolean,
    isChecking: Boolean,
    message: String?,
    onResendClick: () -> Unit,
    onAlreadyVerifiedClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 100.dp, bottom = 24.dp),
    ) {
        Text(text = "📧", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Verificá tu email", style = PsType.H2, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Te mandamos un email a $email con un link para confirmar tu cuenta. Abrilo y después volvé acá.",
            style = PsType.BodySmall,
            color = PsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )

        message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, style = PsType.BodySmall, color = PsColor.TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(32.dp))
        PsButton(
            text = "Ya verifiqué",
            onClick = onAlreadyVerifiedClick,
            variant = PsButtonVariant.Primary,
            enabled = !isChecking && !isResending,
            isLoading = isChecking,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        PsButton(
            text = "Reenviar email",
            onClick = onResendClick,
            variant = PsButtonVariant.Secondary,
            enabled = !isChecking && !isResending,
            isLoading = isResending,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Volver",
            style = PsType.BodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onBackClick),
        )
    }
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 100.dp, bottom = 24.dp),
    ) {
        Text(text = "🌱", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Pasitos", style = PsType.H1)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Seguimiento y control del niño sano",
            style = PsType.BodySmall,
            color = PsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))
        PsInputField(
            label = "Email",
            value = state.email,
            onValueChange = onEmailChange,
            errorText = state.emailError,
            keyboardType = KeyboardType.Email,
            enabled = !state.isLoading,
        )

        Spacer(modifier = Modifier.height(20.dp))
        PsInputField(
            label = "Contraseña",
            value = state.password,
            onValueChange = onPasswordChange,
            errorText = state.passwordError,
            enabled = !state.isLoading,
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eye),
                        contentDescription = if (state.isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = Color.Unspecified,
                    )
                }
            },
        )

        state.generalError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "¿Olvidaste tu contraseña?",
            style = PsType.BodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onForgotPasswordClick),
        )

        Spacer(modifier = Modifier.height(20.dp))
        PsButton(
            text = "Iniciar sesión",
            onClick = onSubmit,
            variant = PsButtonVariant.Primary,
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = PsColor.Border)
            Text(
                text = "o continuá con",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = PsColor.Border)
        }

        Spacer(modifier = Modifier.height(20.dp))
        PsButton(
            text = "Continuar con Google",
            onClick = onGoogleSignInClick,
            variant = PsButtonVariant.Secondary,
            enabled = !state.isLoading,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(end = 10.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.clickable(onClick = onRegisterClick)) {
            Text(text = "¿No tenés cuenta? ", style = PsType.BodySmall, color = PsColor.TextSecondary)
            Text(
                text = "Registrate",
                style = PsType.BodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PsTheme {
        LoginContent(
            state = LoginUiState(email = "familia@ejemplo.com"),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
            onGoogleSignInClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Verificá tu email")
@Composable
private fun EmailVerificationPendingPreview() {
    PsTheme {
        EmailVerificationPendingContent(
            email = "familia@ejemplo.com",
            isResending = false,
            isChecking = false,
            message = null,
            onResendClick = {},
            onAlreadyVerifiedClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Con error")
@Composable
private fun LoginScreenErrorPreview() {
    PsTheme {
        LoginContent(
            state = LoginUiState(
                email = "familia@ejemplo.com",
                password = "incorrecta",
                generalError = "Email o contraseña incorrectos",
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
            onGoogleSignInClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {},
        )
    }
}
