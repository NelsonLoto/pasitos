package com.pasitos.app.feature.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.data.vaccination.model.VaccinationSummary
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.vaccination.DoseStatus

@Composable
fun VaccinationCalendarScreen(
    onChildChipClick: () -> Unit,
    onDoseClick: (childId: String, doseCode: String) -> Unit,
    onMarkPreviousDosesClick: (childId: String) -> Unit,
    onAddOtherVaccineClick: (childId: String) -> Unit,
    onOtherVaccineClick: (childId: String, vaccineId: String) -> Unit,
    viewModel: VaccinationCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    VaccinationCalendarContent(
        state = state,
        onChildChipClick = onChildChipClick,
        onFilterSelected = viewModel::onFilterSelected,
        onDoseClick = { doseCode -> state.childId?.let { onDoseClick(it, doseCode) } },
        onMarkPreviousDosesClick = { state.childId?.let(onMarkPreviousDosesClick) },
        onAddOtherVaccineClick = { state.childId?.let(onAddOtherVaccineClick) },
        onOtherVaccineClick = { vaccineId -> state.childId?.let { onOtherVaccineClick(it, vaccineId) } },
    )
}

@Composable
private fun VaccinationCalendarContent(
    state: VaccinationCalendarUiState,
    onChildChipClick: () -> Unit,
    onFilterSelected: (DoseFilter) -> Unit,
    onDoseClick: (doseCode: String) -> Unit,
    onMarkPreviousDosesClick: () -> Unit,
    onAddOtherVaccineClick: () -> Unit,
    onOtherVaccineClick: (vaccineId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 30.dp, bottom = 14.dp),
        ) {
            Text(text = "Vacunas", style = PsType.H3)
            if (state.childName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onChildChipClick),
                ) {
                    Text(text = state.childName, style = PsType.BodySmall)
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = null,
                        tint = PsColor.TextPrimary,
                        modifier = Modifier
                            .padding(start = 3.dp)
                            .size(14.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            when {
                !state.isLoaded -> Unit
                state.childId == null -> EmptyMessage("Agregá un hijo para ver su calendario de vacunación.")
                !state.hasCalendar -> EmptyMessage("Todavía no hay un calendario cargado. Se actualiza solo cuando haya conexión.")
                else -> CalendarBody(state, onFilterSelected, onDoseClick, onMarkPreviousDosesClick, onAddOtherVaccineClick, onOtherVaccineClick)
            }
        }
    }
}

@Composable
private fun CalendarBody(
    state: VaccinationCalendarUiState,
    onFilterSelected: (DoseFilter) -> Unit,
    onDoseClick: (doseCode: String) -> Unit,
    onMarkPreviousDosesClick: () -> Unit,
    onAddOtherVaccineClick: () -> Unit,
    onOtherVaccineClick: (vaccineId: String) -> Unit,
) {
    state.summary?.let { SummaryCard(it) }

    Spacer(modifier = Modifier.height(12.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        DoseFilter.entries.forEach { filter ->
            FilterChip(
                selected = state.filter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.label, style = PsType.Caption) },
            )
        }
    }

    if (state.groups.isEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "No hay dosis en este estado.", style = PsType.BodySmall, color = PsColor.TextSecondary)
    }
    state.groups.forEach { group ->
        SectionTitle(group.label)
        PsCard(modifier = Modifier.fillMaxWidth()) {
            group.doses.forEachIndexed { index, dose ->
                DoseRow(dose = dose, showDivider = index < group.doses.lastIndex, onClick = { onDoseClick(dose.doseCode) })
            }
        }
    }

    if (state.showFormerDoses && state.formerDoses.isNotEmpty()) {
        SectionTitle("Dosis de calendarios anteriores")
        RecordCard(rows = state.formerDoses, onClick = onDoseClick)
    }

    if (state.showOtherVaccines) {
        SectionTitle("Otras vacunas")
        if (state.otherVaccines.isEmpty()) {
            Text(
                text = "Las vacunas que no están en el Calendario Nacional, como la antigripal, se registran acá.",
                style = PsType.Caption,
                color = PsColor.TextSecondary,
            )
        } else {
            RecordCard(rows = state.otherVaccines, onClick = onOtherVaccineClick)
        }

        Spacer(modifier = Modifier.height(20.dp))
        if (state.canMarkPreviousDoses) {
            PsButton(
                text = "Marcar dosis anteriores",
                onClick = onMarkPreviousDosesClick,
                variant = PsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        PsButton(
            text = "Registrar otra vacuna",
            onClick = onAddOtherVaccineClick,
            variant = PsButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryCard(summary: VaccinationSummary) {
    val tone = when {
        summary.overdue > 0 -> PsAlertTone.Error
        summary.due > 0 -> PsAlertTone.Warning
        else -> PsAlertTone.Info
    }
    AlertCard(text = summary.text, tone = tone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = PsType.BodySmall,
        color = PsColor.TextSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun DoseRow(dose: DoseRowUi, showDivider: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = dose.vaccineName, style = PsType.BodySmall)
            Text(text = dose.detail, style = PsType.Caption, color = PsColor.TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        DoseStatusBadge(status = dose.status)
    }
    if (showDivider) {
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(PsColor.Border))
    }
}

@Composable
private fun RecordCard(rows: List<RecordRowUi>, onClick: (id: String) -> Unit) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(row.id) }
                    .padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = row.title, style = PsType.BodySmall)
                    Text(text = row.detail, style = PsType.Caption, color = PsColor.TextSecondary)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PsColor.TextSecondary,
                )
            }
            if (index < rows.lastIndex) {
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(PsColor.Border))
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(text = text, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 24.dp))
}

@Preview(showBackground = true)
@Composable
private fun VaccinationCalendarScreenPreview() {
    PsTheme {
        VaccinationCalendarContent(
            state = VaccinationCalendarUiState(
                childId = "lucia",
                childName = "Lucía",
                isLoaded = true,
                hasCalendar = true,
                summary = VaccinationSummary(overdue = 1, due = 1, upcoming = 0),
                groups = listOf(
                    AgeGroupUi(
                        "Al nacer",
                        listOf(
                            DoseRowUi("BCG", "BCG", "Única dosis · 16/10/2025", DoseStatus.APPLIED),
                            DoseRowUi("HB-NEO", "Hepatitis B", "Dosis neonatal · Fecha no registrada", DoseStatus.APPLIED),
                        ),
                    ),
                    AgeGroupUi("2 meses", listOf(DoseRowUi("NEUMO-1", "Neumococo conjugada", "1ª dosis · Desde el 16/12/2025", DoseStatus.OVERDUE))),
                    AgeGroupUi("12 meses", listOf(DoseRowUi("HA", "Hepatitis A", "Única dosis · Desde el 16/10/2026", DoseStatus.DUE))),
                    AgeGroupUi("15 meses", listOf(DoseRowUi("VAR-1", "Varicela", "1ª dosis · 16/01/2027", DoseStatus.SCHEDULED))),
                ),
                otherVaccines = listOf(RecordRowUi("1", "Antigripal 2026", "01/05/2026")),
                canMarkPreviousDoses = true,
            ),
            onChildChipClick = {},
            onFilterSelected = {},
            onDoseClick = {},
            onMarkPreviousDosesClick = {},
            onAddOtherVaccineClick = {},
            onOtherVaccineClick = {},
        )
    }
}
