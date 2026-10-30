package com.pasitos.app.feature.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsListRow
import com.pasitos.app.core.designsystem.component.PsSegmentedControl
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate

@Composable
fun MeasurementFormScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    onOpenMeasurement: (measurementId: String) -> Unit,
    viewModel: MeasurementFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onDone() }
    }
    LaunchedEffect(Unit) {
        viewModel.openMeasurement.collect(onOpenMeasurement)
    }

    MeasurementFormContent(
        state = state,
        onBackClick = onBackClick,
        onDateSelected = viewModel::onDateSelected,
        onStandingSelected = viewModel::onStandingSelected,
        onWeightChange = viewModel::onWeightChange,
        onLengthChange = viewModel::onLengthChange,
        onHeadCircumferenceChange = viewModel::onHeadCircumferenceChange,
        onSave = viewModel::onSave,
        onDeleteClick = viewModel::onDeleteClick,
        onDiscardChange = viewModel::onDiscardChange,
    )

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(text = "¿Eliminar esta medición?") },
            text = { Text(text = "Se borra del historial y de las curvas.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text(text = "Cancelar") } },
        )
    }

    state.duplicate?.let { duplicate ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDuplicate,
            title = { Text(text = "Ya cargaste una medición el ${duplicate.dateText}") },
            text = { Text(text = "Hay una sola por fecha. Podés editar esa medición; lo que cargaste acá no se guarda.") },
            confirmButton = { TextButton(onClick = viewModel::onEditDuplicate) { Text(text = "Editar esa medición") } },
            dismissButton = { TextButton(onClick = viewModel::onDismissDuplicate) { Text(text = "Cancelar") } },
        )
    }
}

@Composable
private fun MeasurementFormContent(
    state: MeasurementFormUiState,
    onBackClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStandingSelected: (Boolean) -> Unit,
    onWeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onHeadCircumferenceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDiscardChange: () -> Unit,
) {
    val title = when (state.mode) {
        MeasurementFormMode.NEW -> "Registrar medición"
        MeasurementFormMode.EDIT -> "Editar medición"
        MeasurementFormMode.READ_ONLY -> "Medición"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = title, showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            when {
                state.notFound -> Text(
                    text = "Esta medición ya no está en el historial.",
                    style = PsType.BodySmall,
                    color = PsColor.TextSecondary,
                )
                !state.isLoaded -> Unit
                state.mode == MeasurementFormMode.READ_ONLY -> ReadOnlyMeasurement(state)
                // RF-CRE-01, CA6
                !state.canEdit -> Text(
                    text = "Este perfil está finalizado: ya no admite mediciones nuevas.",
                    style = PsType.BodySmall,
                    color = PsColor.TextSecondary,
                )
                else -> MeasurementForm(
                    state = state,
                    onDateSelected = onDateSelected,
                    onStandingSelected = onStandingSelected,
                    onWeightChange = onWeightChange,
                    onLengthChange = onLengthChange,
                    onHeadCircumferenceChange = onHeadCircumferenceChange,
                    onSave = onSave,
                    onDeleteClick = onDeleteClick,
                    onDiscardChange = onDiscardChange,
                )
            }
        }
    }
}

/** RF-CRE-02, CA2: la medición de otra persona, sin edición y con su autor. */
@Composable
private fun ReadOnlyMeasurement(state: MeasurementFormUiState) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        state.readOnlyRows.forEachIndexed { index, (label, value) ->
            PsListRow(label = label, value = value, showDivider = index < state.readOnlyRows.lastIndex)
        }
        state.authorText?.let {
            Text(text = it, style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ColumnScope.MeasurementForm(
    state: MeasurementFormUiState,
    onDateSelected: (LocalDate) -> Unit,
    onStandingSelected: (Boolean) -> Unit,
    onWeightChange: (String) -> Unit,
    onLengthChange: (String) -> Unit,
    onHeadCircumferenceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDiscardChange: () -> Unit,
) {
    val enabled = !state.isSaving
    when (state.syncState) {
        SyncState.ERROR -> {
            AlertCard(
                text = "No se pudo guardar este cambio: ${state.syncError}. Corregilo y guardá de nuevo, o descartalo.",
                tone = PsAlertTone.Error,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onDiscardChange, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Descartar cambio")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        SyncState.PENDING -> {
            Text(
                text = "Pendiente de sincronizar: se envía cuando haya conexión.",
                style = PsType.Caption,
                color = PsColor.TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        SyncState.SYNCED, null -> Unit
    }

    PsDatePickerField(
        label = "Fecha",
        selectedDate = state.date,
        onDateSelected = onDateSelected,
        errorText = state.dateError,
        enabled = enabled,
        minDate = state.minDate,
        maxDate = state.maxDate,
    )

    Spacer(modifier = Modifier.height(16.dp))
    PsInputField(
        label = "Peso (kg)",
        value = state.weightKg,
        onValueChange = onWeightChange,
        placeholder = "Ej: 7,8",
        errorText = state.weightError,
        keyboardType = KeyboardType.Decimal,
        enabled = enabled,
    )

    Spacer(modifier = Modifier.height(16.dp))
    PsInputField(
        label = state.lengthLabel,
        value = state.lengthCm,
        onValueChange = onLengthChange,
        placeholder = "Ej: 68,5",
        errorText = state.lengthError,
        keyboardType = KeyboardType.Decimal,
        enabled = enabled,
    )
    Spacer(modifier = Modifier.height(8.dp))
    if (state.offersLying) {
        // RF-CRE-10, CA1 y CA2: la forma que corresponde a la edad viene seleccionada
        PsSegmentedControl(
            options = listOf("Acostado", "De pie"),
            selectedIndex = if (state.standing) 1 else 0,
            onSelect = { index -> if (enabled) onStandingSelected(index == 1) },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        // RF-CRE-10, CA3
        Text(text = "Desde los 5 años la talla se mide de pie.", style = PsType.Caption, color = PsColor.TextSecondary)
    }
    Text(
        text = state.lengthHelp,
        style = PsType.Caption,
        color = PsColor.TextSecondary,
        modifier = Modifier.padding(top = 6.dp),
    )

    if (state.offersHeadCircumference) {
        Spacer(modifier = Modifier.height(16.dp))
        PsInputField(
            label = "Perímetro cefálico (cm)",
            value = state.headCircumferenceCm,
            onValueChange = onHeadCircumferenceChange,
            placeholder = "Ej: 44",
            errorText = state.headCircumferenceError,
            keyboardType = KeyboardType.Decimal,
            enabled = enabled,
        )
    }

    state.generalError?.let { error ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.height(20.dp))
    PsButton(
        text = if (state.mode == MeasurementFormMode.EDIT) "Guardar cambios" else "Guardar medición",
        onClick = onSave,
        variant = PsButtonVariant.Primary,
        enabled = enabled,
        isLoading = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.mode == MeasurementFormMode.EDIT) {
        TextButton(onClick = onDeleteClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementFormScreenPreview() {
    PsTheme {
        MeasurementFormContent(
            state = MeasurementFormUiState(
                isLoaded = true,
                date = LocalDate.of(2026, 10, 19),
                maxDate = LocalDate.of(2026, 10, 19),
                weightKg = "7,8",
                lengthCm = "68",
            ),
            onBackClick = {},
            onDateSelected = {},
            onStandingSelected = {},
            onWeightChange = {},
            onLengthChange = {},
            onHeadCircumferenceChange = {},
            onSave = {},
            onDeleteClick = {},
            onDiscardChange = {},
        )
    }
}
