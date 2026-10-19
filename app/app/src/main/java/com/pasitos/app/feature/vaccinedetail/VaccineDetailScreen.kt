package com.pasitos.app.feature.vaccinedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.feature.vaccination.DoseStatusBadge
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.api.vaccination.MAX_LOT_LENGTH
import com.pasitos.shared.api.vaccination.MAX_PLACE_LENGTH
import com.pasitos.shared.vaccination.DoseStatus
import java.time.LocalDate

/** Mensaje ante una dosis atrasada (RN-VAC-05): la app no calcula esquemas de recuperación. */
private const val OVERDUE_MESSAGE = "Esta dosis está atrasada. Consultá con tu pediatra o en el vacunatorio cómo completar el esquema."

@Composable
fun VaccineDetailScreen(
    onBackClick: () -> Unit,
    onDone: () -> Unit,
    viewModel: VaccineDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.done.collect { onDone() }
    }

    VaccineDetailContent(
        state = state,
        onBackClick = onBackClick,
        onDateSelected = viewModel::onDateSelected,
        onLotChange = viewModel::onLotChange,
        onPlaceChange = viewModel::onPlaceChange,
        onSave = viewModel::onSave,
        onEditClick = viewModel::onEditClick,
        onCancelEdit = viewModel::onCancelEdit,
        onDeleteClick = viewModel::onDeleteClick,
    )

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(text = "¿Eliminar el registro de esta dosis?") },
            text = { Text(text = "La dosis vuelve a figurar como no aplicada en el calendario.") },
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
private fun VaccineDetailContent(
    state: VaccineDetailUiState,
    onBackClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onLotChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onSave: () -> Unit,
    onEditClick: () -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = state.vaccineName, showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            if (state.notFound) {
                Text(text = "Esta dosis ya no está en el calendario de este niño o niña.", style = PsType.BodySmall, color = PsColor.TextSecondary)
                return@Column
            }
            if (!state.isLoaded) return@Column

            if (state.doseLabel.isNotBlank()) Text(text = state.doseLabel, style = PsType.BodyMedium)
            state.status?.let {
                Spacer(modifier = Modifier.height(8.dp))
                DoseStatusBadge(status = it)
            }
            if (state.isOverdue) {
                Spacer(modifier = Modifier.height(12.dp))
                AlertCard(text = OVERDUE_MESSAGE, tone = PsAlertTone.Error, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(16.dp))
            PsCard(modifier = Modifier.fillMaxWidth()) {
                state.indicatedText?.let { PsListRow(label = "Edad indicada", value = it) }
                state.prevents?.let {
                    Text(text = "Previene", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    Text(text = it, style = PsType.BodySmall)
                }
                state.source?.let {
                    Text(text = "Fuente", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    Text(text = it, style = PsType.BodySmall)
                }
                if (state.indicatedText == null) {
                    Text(text = "Esta dosis ya no está en el calendario vigente. Se conserva tal como se registró.", style = PsType.Caption, color = PsColor.TextSecondary)
                }
            }

            val applied = state.applied
            if (applied != null && !state.isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                PsCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Aplicación", style = PsType.BodyMedium)
                    PsListRow(label = "Fecha", value = applied.dateText)
                    PsListRow(label = "Lote", value = applied.lot ?: "Sin datos")
                    PsListRow(label = "Lugar", value = applied.place ?: "Sin datos", showDivider = false)
                    Text(text = applied.authorText, style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                }
            }

            if (state.showForm) {
                Spacer(modifier = Modifier.height(16.dp))
                DoseForm(state, onDateSelected, onLotChange, onPlaceChange)
            }

            if (!state.isOnline && (state.showForm || applied?.isOwn == true)) {
                Spacer(modifier = Modifier.height(16.dp))
                AlertCard(
                    text = "Necesitás conexión a internet para registrar, editar o eliminar una dosis.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))
            when {
                state.showForm -> {
                    PsButton(
                        text = if (state.isEditing) "Guardar cambios" else "Guardar aplicación",
                        onClick = onSave,
                        enabled = state.isOnline && !state.isSaving,
                        isLoading = state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.isEditing) {
                        TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text(text = "Cancelar") }
                    }
                }
                applied?.isOwn == true -> {
                    PsButton(
                        text = "Editar",
                        onClick = onEditClick,
                        variant = PsButtonVariant.Secondary,
                        enabled = state.isOnline && !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onDeleteClick, enabled = state.isOnline && !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Eliminar", color = if (state.isOnline) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseForm(
    state: VaccineDetailUiState,
    onDateSelected: (LocalDate) -> Unit,
    onLotChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
) {
    Text(text = if (state.isEditing) "Editar aplicación" else "Registrar aplicación", style = PsType.BodyMedium)
    Spacer(modifier = Modifier.height(12.dp))
    PsDatePickerField(
        label = if (state.dateRequired) "Fecha de aplicación" else "Fecha de aplicación (opcional)",
        selectedDate = state.form.date,
        onDateSelected = onDateSelected,
        errorText = state.form.dateError,
        enabled = !state.isSaving,
        minDate = state.minDate,
        maxDate = state.maxDate,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PsInputField(
        label = "Lote (opcional)",
        value = state.form.lot,
        onValueChange = onLotChange,
        placeholder = "Ej: A1234B",
        errorText = state.form.lotError,
        enabled = !state.isSaving,
    )
    Counter(text = "Figura en el carnet de vacunación", length = state.form.lot.trim().length, max = MAX_LOT_LENGTH)
    Spacer(modifier = Modifier.height(12.dp))
    PsInputField(
        label = "Vacunatorio o lugar (opcional)",
        value = state.form.place,
        onValueChange = onPlaceChange,
        errorText = state.form.placeError,
        enabled = !state.isSaving,
    )
    Counter(text = null, length = state.form.place.trim().length, max = MAX_PLACE_LENGTH)
}

/** Texto de ayuda y contador de caracteres debajo de un campo. */
@Composable
private fun Counter(text: String?, length: Int, max: Int) {
    Text(
        text = listOfNotNull(text, "$length/$max").joinToString(" · "),
        style = PsType.Caption,
        color = if (length > max) MaterialTheme.colorScheme.error else PsColor.TextSecondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun VaccineDetailScreenPreview() {
    PsTheme {
        VaccineDetailContent(
            state = VaccineDetailUiState(
                isLoaded = true,
                vaccineName = "Neumococo conjugada",
                doseLabel = "1ª dosis",
                status = DoseStatus.OVERDUE,
                indicatedText = "2 meses · 16/12/2025",
                prevents = "Meningitis, neumonía y sepsis por neumococo",
                source = "Calendario Nacional de Vacunación 2026, Ministerio de Salud de la Nación",
                minDate = LocalDate.of(2025, 10, 16),
                maxDate = LocalDate.of(2026, 10, 19),
            ),
            onBackClick = {},
            onDateSelected = {},
            onLotChange = {},
            onPlaceChange = {},
            onSave = {},
            onEditClick = {},
            onCancelEdit = {},
            onDeleteClick = {},
        )
    }
}
