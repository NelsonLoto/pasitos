package com.ninosano.app.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ninosano.app.R
import com.ninosano.app.core.designsystem.component.NsButton
import com.ninosano.app.core.designsystem.component.NsButtonVariant
import com.ninosano.app.core.designsystem.component.NsInputField
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
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
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(text = "Niño Sano", style = NsType.H1)
        Text(
            text = "Iniciá sesión para seguir el crecimiento de tus hijos",
            style = NsType.BodySmall,
            color = NsColor.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )

        NsInputField(
            label = "Email",
            value = state.email,
            onValueChange = onEmailChange,
            errorText = state.emailError,
            keyboardType = KeyboardType.Email,
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(16.dp))
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
                        imageVector = if (state.isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (state.isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
        )

        state.generalError?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, style = NsType.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        NsButton(
            text = "Iniciar sesión",
            onClick = onSubmit,
            variant = NsButtonVariant.Primary,
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = NsColor.Border)
            Text(
                text = "  o continuá con  ",
                style = NsType.Caption,
                color = NsColor.TextSecondary,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = NsColor.Border)
        }

        Spacer(modifier = Modifier.height(16.dp))
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
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
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
        )
    }
}
