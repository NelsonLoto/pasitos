package com.pasitos.app.feature.profile.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.ui.theme.PsTheme

/** RF-CTA-11: al guardar vuelve al perfil, que ya muestra el nombre nuevo. */
@Composable
fun EditNameScreen(
    onBackClick: () -> Unit,
    viewModel: EditNameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onBackClick() }
    }

    EditNameContent(
        state = state,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onSave = viewModel::onSave,
        onBackClick = onBackClick,
    )
}

@Composable
private fun EditNameContent(
    state: EditNameUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
) {
    AccountFormScaffold(title = "Nombre y apellido", isOnline = state.isOnline, error = state.error, onBackClick = onBackClick) {
        PsInputField(
            label = "Nombre",
            value = state.firstName,
            onValueChange = onFirstNameChange,
            errorText = state.firstNameError,
            enabled = !state.isSaving,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PsInputField(
            label = "Apellido",
            value = state.lastName,
            onValueChange = onLastNameChange,
            errorText = state.lastNameError,
            enabled = !state.isSaving,
        )
        Spacer(modifier = Modifier.height(24.dp))
        PsButton(
            text = "Guardar",
            onClick = onSave,
            enabled = state.canSave,
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditNamePreview() {
    PsTheme {
        EditNameContent(
            state = EditNameUiState(firstName = "María", lastName = "Gómez"),
            onFirstNameChange = {},
            onLastNameChange = {},
            onSave = {},
            onBackClick = {},
        )
    }
}
