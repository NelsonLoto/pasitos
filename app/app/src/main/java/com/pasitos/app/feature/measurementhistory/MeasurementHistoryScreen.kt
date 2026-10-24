package com.pasitos.app.feature.measurementhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.core.sync.SyncState
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun MeasurementHistoryScreen(
    onBackClick: () -> Unit,
    onMeasurementClick: (childId: String, measurementId: String) -> Unit,
    onAddClick: (childId: String) -> Unit,
    viewModel: MeasurementHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MeasurementHistoryContent(
        state = state,
        onBackClick = onBackClick,
        onMeasurementClick = { onMeasurementClick(viewModel.childId, it) },
        onAddClick = { onAddClick(viewModel.childId) },
    )
}

@Composable
private fun MeasurementHistoryContent(
    state: MeasurementHistoryUiState,
    onBackClick: () -> Unit,
    onMeasurementClick: (measurementId: String) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Historial de mediciones", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.canAdd) {
                PsButton(text = "Registrar medición", onClick = onAddClick, modifier = Modifier.fillMaxWidth())
            }
            if (state.rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Todavía no hay mediciones registradas.",
                        style = PsType.BodySmall,
                        color = PsColor.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            state.rows.forEach { row ->
                MeasurementRow(row = row, onClick = row.measurementId?.let { id -> { onMeasurementClick(id) } })
            }
        }
    }
}

@Composable
private fun MeasurementRow(row: MeasurementHistoryRowUi, onClick: (() -> Unit)?) {
    PsCard(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(text = row.dateText, style = PsType.BodySmall)
                Text(text = row.ageText, style = PsType.Caption, color = PsColor.TextSecondary)
                row.authorText?.let { Text(text = it, style = PsType.Caption, color = PsColor.TextSecondary) }
            }
            Column(horizontalAlignment = Alignment.End) {
                row.primaryValue?.let { Text(text = it, style = PsType.H3, color = PsColor.TextPrimary) }
                row.secondaryValues?.let { Text(text = it, style = PsType.Caption, color = PsColor.TextSecondary) }
            }
        }
        // RNF-OFF-04: lo que todavía no llegó al servidor se marca
        when (row.syncState) {
            SyncState.PENDING -> PsBadge(text = "Pendiente de sincronizar", tone = PsBadgeTone.Neutral, modifier = Modifier.padding(top = 8.dp))
            SyncState.ERROR -> PsBadge(text = "No se pudo guardar", tone = PsBadgeTone.Error, modifier = Modifier.padding(top = 8.dp))
            SyncState.SYNCED -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeasurementHistoryScreenPreview() {
    PsTheme {
        MeasurementHistoryContent(
            state = MeasurementHistoryUiState(
                childName = "Lucía",
                canAdd = true,
                rows = listOf(
                    MeasurementHistoryRowUi("1", "12/08/2026", "10 meses", "7,8 kg", "68 cm acostado · PC 44 cm", "Familia", SyncState.PENDING),
                    MeasurementHistoryRowUi("2", "14/07/2026", "9 meses", "7,3 kg", "66 cm acostado", "Familia"),
                    MeasurementHistoryRowUi(null, "16/10/2025", "Al nacer", "3,25 kg", "49 cm · PC 34 cm", null),
                ),
            ),
            onBackClick = {},
            onMeasurementClick = {},
            onAddClick = {},
        )
    }
}
