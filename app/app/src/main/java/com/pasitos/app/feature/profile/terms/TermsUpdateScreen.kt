package com.pasitos.app.feature.profile.terms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsTermsCheckbox
import com.pasitos.app.feature.profile.PendingChangesDialog
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** RN-CTA-02: aparece sobre toda la app mientras haya una versión de los términos sin aceptar. */
@Composable
fun TermsUpdateScreen(viewModel: TermsUpdateViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // No hay a dónde volver: o acepta, o cierra sesión
    BackHandler {}

    // Si deja de mostrarse por otro motivo (por ejemplo, se aceptaron en otro teléfono), la próxima
    // vez vuelve a empezar sin marcar
    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

    TermsUpdateContent(
        state = state,
        onAcceptTermsChange = viewModel::onAcceptTermsChange,
        onAcceptClick = viewModel::onAcceptClick,
        onLogoutClick = viewModel::onLogoutClick,
    )

    state.pendingChangesToConfirm?.let { pending ->
        PendingChangesDialog(pending = pending, onConfirm = viewModel::onConfirmLogout, onDismiss = viewModel::onDismissLogout)
    }
}

@Composable
private fun TermsUpdateContent(
    state: TermsUpdateUiState,
    onAcceptTermsChange: (Boolean) -> Unit,
    onAcceptClick: () -> Unit,
    onLogoutClick: () -> Unit,
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
        Text(text = "📄", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Actualizamos los términos", style = PsType.H2, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Cambiaron los Términos y Condiciones o la Política de Privacidad. Para seguir usando Pasitos tenés que aceptar la versión nueva.",
            style = PsType.BodySmall,
            color = PsColor.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))
        PsTermsCheckbox(
            checked = state.acceptTerms,
            onCheckedChange = onAcceptTermsChange,
            enabled = !state.isLoading,
            errorText = state.termsError,
        )

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        PsButton(
            text = "Aceptar y continuar",
            onClick = onAcceptClick,
            variant = PsButtonVariant.Primary,
            enabled = !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Cerrar sesión",
            style = PsType.BodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable(enabled = !state.isLoading, onClick = onLogoutClick),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsUpdatePreview() {
    PsTheme {
        TermsUpdateContent(state = TermsUpdateUiState(), onAcceptTermsChange = {}, onAcceptClick = {}, onLogoutClick = {})
    }
}
