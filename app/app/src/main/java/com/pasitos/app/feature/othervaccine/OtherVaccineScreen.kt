package com.pasitos.app.feature.othervaccine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsListRow
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.feature.vaccination.VaccinationDateFormatter
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.api.vaccination.MAX_LOT_LENGTH
import com.pasitos.shared.api.vaccination.MAX_OTHER_VACCINE_NOTE_LENGTH
import java.time.LocalDate

@Composable
fun OtherVaccineScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: OtherVaccineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onDone() }
    }

    OtherVaccineContent(
        state = state,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onSuggestion = viewModel::onSuggestion,
        onDateSelected = viewModel::onDateSelected,
        onLotChange = viewModel::onLotChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
        onDeleteClick = viewModel::onDeleteClick,
    )

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(text = "¿Eliminar esta vacuna?") },
            text = { Text(text = "Se borra de la libreta de vacunas del niño o niña.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text(text = "Cancelar") } },
        )
    }
}

@Composable
private fun OtherVaccineContent(
    state: OtherVaccineUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onLotChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = if (state.isNew) "Registrar otra vacuna" else "Otra vacuna", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            when {
                !state.isLoaded -> Unit
                state.notFound -> Text(text = "Esta vacuna ya no está registrada.", style = PsType.BodySmall, color = PsColor.TextSecondary)
                state.isReadOnly -> ReadOnly(state)
                else -> Form(state, onNameChange, onSuggestion, onDateSelected, onLotChange, onNoteChange, onSave, onDeleteClick)
            }
        }
    }
}

@Composable
private fun ReadOnly(state: OtherVaccineUiState) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = state.name, style = PsType.BodyMedium)
        PsListRow(label = "Fecha", value = state.date?.format(VaccinationDateFormatter).orEmpty())
        PsListRow(label = "Lote", value = state.lot.ifBlank { "Sin datos" }, showDivider = false)
        if (state.note.isNotBlank()) {
            Text(text = "Nota", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
            Text(text = state.note, style = PsType.BodySmall)
        }
        state.authorText?.let {
            Text(text = it, style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Form(
    state: OtherVaccineUiState,
    onNameChange: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onLotChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Text(
        text = "Para vacunas que no están en el Calendario Nacional, por ejemplo aplicadas en forma privada. No tienen estado ni avisos.",
        style = PsType.BodySmall,
        color = PsColor.TextSecondary,
    )
    Spacer(modifier = Modifier.height(16.dp))
    PsInputField(
        label = "Nombre de la vacuna",
        value = state.name,
        onValueChange = onNameChange,
        errorText = state.nameError,
        enabled = !state.isSaving,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OtherVaccineSuggestions.forEach { suggestion ->
            SuggestionChip(onClick = { onSuggestion(suggestion) }, label = { Text(text = suggestion, style = PsType.Caption) }, enabled = !state.isSaving)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    PsDatePickerField(
        label = "Fecha de aplicación",
        selectedDate = state.date,
        onDateSelected = onDateSelected,
        errorText = state.dateError,
        enabled = !state.isSaving,
        minDate = state.minDate,
        maxDate = state.maxDate,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PsInputField(label = "Lote (opcional)", value = state.lot, onValueChange = onLotChange, errorText = state.lotError, enabled = !state.isSaving)
    Counter(length = state.lot.trim().length, max = MAX_LOT_LENGTH)
    Spacer(modifier = Modifier.height(12.dp))
    PsInputField(
        label = "Nota (opcional)",
        value = state.note,
        onValueChange = onNoteChange,
        errorText = state.noteError,
        enabled = !state.isSaving,
        singleLine = false,
    )
    Counter(length = state.note.trim().length, max = MAX_OTHER_VACCINE_NOTE_LENGTH)

    if (!state.isOnline) {
        Spacer(modifier = Modifier.height(16.dp))
        AlertCard(text = "Necesitás conexión a internet para guardar o eliminar la vacuna.", tone = PsAlertTone.Warning, modifier = Modifier.fillMaxWidth())
    }
    state.error?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = it, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(20.dp))
    PsButton(
        text = "Guardar",
        onClick = onSave,
        enabled = state.isOnline && !state.isSaving,
        isLoading = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )
    if (!state.isNew) {
        TextButton(onClick = onDeleteClick, enabled = state.isOnline && !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Eliminar", color = if (state.isOnline) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
        }
    }
}

@Composable
private fun Counter(length: Int, max: Int) {
    Text(
        text = "$length/$max",
        style = PsType.Caption,
        color = if (length > max) MaterialTheme.colorScheme.error else PsColor.TextSecondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun OtherVaccineScreenPreview() {
    PsTheme {
        OtherVaccineContent(
            state = OtherVaccineUiState(
                isLoaded = true,
                name = "Antigripal",
                date = LocalDate.of(2026, 5, 1),
                minDate = LocalDate.of(2025, 10, 16),
                maxDate = LocalDate.of(2026, 10, 19),
            ),
            onBackClick = {},
            onNameChange = {},
            onSuggestion = {},
            onDateSelected = {},
            onLotChange = {},
            onNoteChange = {},
            onSave = {},
            onDeleteClick = {},
        )
    }
}
