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
import com.pasitos.app.core.designsystem.component.NsButton
import com.pasitos.app.core.designsystem.component.NsButtonVariant
import com.pasitos.app.core.designsystem.component.NsInputField
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onLoginSuccess() }
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onGoogleSignInClick = viewModel::onGoogleSignInClick,
        onRegisterClick = onRegisterClick,
        // "¿Olvidaste tu contraseña?" no tiene pantalla propia entre las 15 del documento
        // funcional todavía — el link está en el diseño pero sin destino definido.
        onForgotPasswordClick = {},
    )
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
        Text(text = "Pasitos", style = NsType.H1)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Seguimiento y control del niño sano",
            style = NsType.BodySmall,
            color = NsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(36.dp))
        NsInputField(
            label = "Email",
            value = state.email,
            onValueChange = onEmailChange,
            errorText = state.emailError,
            keyboardType = KeyboardType.Email,
            enabled = !state.isLoading,
        )

        Spacer(modifier = Modifier.height(20.dp))
        NsInputField(
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
            Text(text = error, style = NsType.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "¿Olvidaste tu contraseña?",
            style = NsType.BodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onForgotPasswordClick),
        )

        Spacer(modifier = Modifier.height(20.dp))
        NsButton(
            text = "Iniciar sesión",
            onClick = onSubmit,
            variant = NsButtonVariant.Primary,
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NsColor.Border)
            Text(
                text = "o continuá con",
                style = NsType.BodySmall,
                color = NsColor.TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = NsColor.Border)
        }

        Spacer(modifier = Modifier.height(20.dp))
        NsButton(
            text = "Continuar con Google",
            onClick = onGoogleSignInClick,
            variant = NsButtonVariant.Secondary,
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
            Text(text = "¿No tenés cuenta? ", style = NsType.BodySmall, color = NsColor.TextSecondary)
            Text(
                text = "Registrate",
                style = NsType.BodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    NsTheme {
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

@Preview(showBackground = true, name = "Con error")
@Composable
private fun LoginScreenErrorPreview() {
    NsTheme {
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
