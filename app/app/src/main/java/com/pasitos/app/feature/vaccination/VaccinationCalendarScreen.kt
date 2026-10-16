package com.pasitos.app.feature.vaccination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.data.vaccination.model.DoseStatus
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun VaccinationCalendarScreen(
    onChildChipClick: () -> Unit,
    onDoseClick: (vaccineId: String) -> Unit,
    viewModel: VaccinationCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    VaccinationCalendarContent(state = state, onChildChipClick = onChildChipClick, onDoseClick = onDoseClick)
}

@Composable
private fun VaccinationCalendarContent(
    state: VaccinationCalendarUiState,
    onChildChipClick: () -> Unit,
    onDoseClick: (vaccineId: String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            state.doses.forEach { dose ->
                VaccineDoseRow(dose = dose, onClick = { onDoseClick(dose.id) })
            }
        }
    }
}

@Composable
private fun VaccineDoseRow(dose: VaccineDoseUiModel, onClick: () -> Unit) {
    PsCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(text = dose.displayName, style = PsType.BodySmall)
                Text(text = dose.ageLabel, style = PsType.Caption, color = PsColor.TextSecondary)
            }
            PsBadge(text = statusLabel(dose.status), tone = statusTone(dose.status))
        }
    }
}

private fun statusLabel(status: DoseStatus): String = when (status) {
    DoseStatus.Applied -> "Aplicada"
    DoseStatus.Overdue -> "Atrasada"
    DoseStatus.Pending -> "Pendiente"
}

private fun statusTone(status: DoseStatus): PsBadgeTone = when (status) {
    DoseStatus.Applied -> PsBadgeTone.Success
    DoseStatus.Overdue -> PsBadgeTone.Error
    DoseStatus.Pending -> PsBadgeTone.Warning
}

@Preview(showBackground = true)
@Composable
private fun VaccinationCalendarScreenPreview() {
    PsTheme {
        VaccinationCalendarContent(
            state = VaccinationCalendarUiState(
                childName = "Lucía",
                doses = listOf(
                    VaccineDoseUiModel("bcg", "BCG", "Al nacer", DoseStatus.Applied),
                    VaccineDoseUiModel("penta-3", "Pentavalente (3ª dosis)", "6 meses", DoseStatus.Overdue),
                    VaccineDoseUiModel("flu", "Antigripal", "6 meses", DoseStatus.Pending),
                ),
            ),
            onChildChipClick = {},
            onDoseClick = {},
        )
    }
}
