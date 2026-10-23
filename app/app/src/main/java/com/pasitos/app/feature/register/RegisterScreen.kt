package com.pasitos.app.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    onRegisterSuccess: (email: String) -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToEmailVerification.collect { email -> onRegisterSuccess(email) }
    }

    RegisterContent(
        state = state,
        onBackClick = onBackClick,
        onAccountTypeChange = viewModel::onAccountTypeChange,
        onDniChange = viewModel::onDniChange,
        onLicenseNumberChange = viewModel::onLicenseNumberChange,
        onSpecialtyChange = viewModel::onSpecialtyChange,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onAcceptTermsChange = viewModel::onAcceptTermsChange,
        onSubmit = viewModel::onSubmit,
        onLoginClick = onLoginClick,
    )
}

@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onBackClick: () -> Unit,
    onAccountTypeChange: (String) -> Unit,
    onDniChange: (String) -> Unit,
    onLicenseNumberChange: (String) -> Unit,
    onSpecialtyChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAcceptTermsChange: (Boolean) -> Unit,
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
            Text("Tipo de cuenta", style = PsType.Caption, color = PsColor.TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AccountTypeOption(
                    label = "Familiar",
                    selected = state.accountType == "FAMILY",
                    enabled = !state.isLoading,
                    onClick = { onAccountTypeChange("FAMILY") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                AccountTypeOption(
                    label = "Profesional",
                    selected = state.accountType == "PROFESSIONAL",
                    enabled = !state.isLoading,
                    onClick = { onAccountTypeChange("PROFESSIONAL") },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (state.accountType == "FAMILY") {
                PsInputField(
                    label = "DNI", value = state.dni, onValueChange = onDniChange,
                    placeholder = "Ej: 12345678", errorText = state.dniError,
                    keyboardType = KeyboardType.Number, enabled = !state.isLoading,
                )
            } else {
                PsInputField(
                    label = "Matrícula", value = state.licenseNumber, onValueChange = onLicenseNumberChange,
                    placeholder = "Ej: MN 123456", errorText = state.licenseNumberError,
                    enabled = !state.isLoading,
                )
                Spacer(modifier = Modifier.height(16.dp))
                PsInputField(
                    label = "Especialidad", value = state.specialty, onValueChange = onSpecialtyChange,
                    placeholder = "Ej: Pediatría", enabled = !state.isLoading,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Nombre",
                value = state.firstName,
                onValueChange = onFirstNameChange,
                placeholder = "Ej: María",
                errorText = state.firstNameError,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Apellido",
                value = state.lastName,
                onValueChange = onLastNameChange,
                placeholder = "Ej: Gómez",
                errorText = state.lastNameError,
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

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.acceptTerms,
                        enabled = !state.isLoading,
                        role = Role.Checkbox,
                        onValueChange = onAcceptTermsChange,
                    ),
            ) {
                Checkbox(checked = state.acceptTerms, onCheckedChange = null, enabled = !state.isLoading)
                Text(
                    text = "Acepto los Términos y Condiciones y la Política de Privacidad",
                    style = PsType.BodySmall,
                    color = PsColor.TextSecondary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            state.termsError?.let { error ->
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

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

@Composable
private fun AccountTypeOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface, shape)
                        .border(1.5.dp, PsColor.Border, shape)
                },
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = PsType.BodyRegular,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else PsColor.TextPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    PsTheme {
        RegisterContent(
            state = RegisterUiState(),
            onBackClick = {},
            onAccountTypeChange = {},
            onDniChange = {},
            onLicenseNumberChange = {},
            onSpecialtyChange = {},
            onFirstNameChange = {},
            onLastNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onAcceptTermsChange = {},
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
                firstName = "María",
                lastName = "Gómez",
                email = "demo@example.com",
                password = "secreta123",
                confirmPassword = "secreta123",
                acceptTerms = true,
                generalError = "Ya existe una cuenta con este email",
            ),
            onBackClick = {},
            onAccountTypeChange = {},
            onDniChange = {},
            onLicenseNumberChange = {},
            onSpecialtyChange = {},
            onFirstNameChange = {},
            onLastNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onAcceptTermsChange = {},
            onSubmit = {},
            onLoginClick = {},
        )
    }
}
