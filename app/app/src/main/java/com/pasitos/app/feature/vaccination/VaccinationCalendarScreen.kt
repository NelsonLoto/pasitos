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
import com.pasitos.app.core.designsystem.component.NsBadge
import com.pasitos.app.core.designsystem.component.NsBadgeTone
import com.pasitos.app.core.designsystem.component.NsCard
import com.pasitos.app.data.vaccination.model.DoseStatus
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

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
            Text(text = "Vacunas", style = NsType.H3)
            if (state.childName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onChildChipClick),
                ) {
                    Text(text = state.childName, style = NsType.BodySmall)
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = null,
                        tint = NsColor.TextPrimary,
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
    NsCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(text = dose.displayName, style = NsType.BodySmall)
                Text(text = dose.ageLabel, style = NsType.Caption, color = NsColor.TextSecondary)
            }
            NsBadge(text = statusLabel(dose.status), tone = statusTone(dose.status))
        }
    }
}

private fun statusLabel(status: DoseStatus): String = when (status) {
    DoseStatus.Applied -> "Aplicada"
    DoseStatus.Overdue -> "Atrasada"
    DoseStatus.Pending -> "Pendiente"
}

private fun statusTone(status: DoseStatus): NsBadgeTone = when (status) {
    DoseStatus.Applied -> NsBadgeTone.Success
    DoseStatus.Overdue -> NsBadgeTone.Error
    DoseStatus.Pending -> NsBadgeTone.Warning
}

@Preview(showBackground = true)
@Composable
private fun VaccinationCalendarScreenPreview() {
    NsTheme {
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
