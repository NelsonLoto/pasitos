package com.pasitos.app.feature.emailverification

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** RF-CTA-01 CA1, RF-CTA-02: pantalla "Verificá tu email" después de crear la cuenta. */
@Composable
fun EmailVerificationScreen(
    onVerified: (hasInvitations: Boolean) -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateNext.collect { hasInvitations -> onVerified(hasInvitations) }
    }

    EmailVerificationContent(
        state = state,
        onResendClick = viewModel::onResendClick,
        onAlreadyVerifiedClick = viewModel::onAlreadyVerifiedClick,
    )
}

@Composable
private fun EmailVerificationContent(
    state: EmailVerificationUiState,
    onResendClick: () -> Unit,
    onAlreadyVerifiedClick: () -> Unit,
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
        Text(text = "Verificá tu email", style = PsType.H1, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Te mandamos un email a ${state.email} con un link para confirmar tu cuenta. " +
                "Abrilo y después volvé acá.",
            style = PsType.BodyRegular,
            color = PsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )

        state.message?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, style = PsType.BodySmall, color = PsColor.TextSecondary, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(36.dp))
        PsButton(
            text = "Ya verifiqué",
            onClick = onAlreadyVerifiedClick,
            variant = PsButtonVariant.Primary,
            enabled = !state.isChecking && !state.isResending,
            isLoading = state.isChecking,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))
        PsButton(
            text = "Reenviar email",
            onClick = onResendClick,
            variant = PsButtonVariant.Secondary,
            enabled = !state.isChecking && !state.isResending,
            isLoading = state.isResending,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmailVerificationScreenPreview() {
    PsTheme {
        EmailVerificationContent(
            state = EmailVerificationUiState(email = "familia@ejemplo.com"),
            onResendClick = {},
            onAlreadyVerifiedClick = {},
        )
    }
}

@Preview(showBackground = true, name = "Todavía no verificó")
@Composable
private fun EmailVerificationScreenMessagePreview() {
    PsTheme {
        EmailVerificationContent(
            state = EmailVerificationUiState(
                email = "familia@ejemplo.com",
                message = "Todavía no verificaste tu email. Revisá tu bandeja de entrada.",
            ),
            onResendClick = {},
            onAlreadyVerifiedClick = {},
        )
    }
}
