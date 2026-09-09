package com.ninosano.app.feature.measurementhistory

import androidx.compose.foundation.background
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
import com.ninosano.app.core.designsystem.component.NsCard
import com.ninosano.app.core.designsystem.component.NsTopAppBar
import com.ninosano.app.data.measurement.model.Measurement
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
import java.time.LocalDate

@Composable
fun MeasurementHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: MeasurementHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MeasurementHistoryContent(state = state, onBackClick = onBackClick)
}

@Composable
private fun MeasurementHistoryContent(state: MeasurementHistoryUiState, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NsTopAppBar(title = "Historial de mediciones", showBack = true, onBackClick = onBackClick)

        if (state.measurements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Todavía no hay mediciones registradas.",
                    style = NsType.BodySmall,
                    color = NsColor.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.measurements.forEach { measurement ->
                    MeasurementRow(measurement)
                }
            }
        }
    }
}

@Composable
private fun MeasurementRow(measurement: Measurement) {
    NsCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = formatDate(measurement.date), style = NsType.BodySmall)

            Column(horizontalAlignment = Alignment.End) {
                measurement.weightKg?.let {
                    Text(text = "${formatDecimal(it)} kg", style = NsType.H3, color = NsColor.TextPrimary)
                }
                val secondaryLine = buildList {
                    measurement.heightCm?.let { add("${formatDecimal(it)} cm") }
                    measurement.headCircumferenceCm?.let { add("PC ${formatDecimal(it)} cm") }
                }.joinToString(" · ")
                if (secondaryLine.isNotBlank()) {
                    Text(text = secondaryLine, style = NsType.Caption, color = NsColor.TextSecondary)
                }
            }
        }
    }
}

private val MonthAbbreviations = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)

private fun formatDate(date: LocalDate): String = "${date.dayOfMonth} ${MonthAbbreviations[date.monthValue - 1]} ${date.year}"

private fun formatDecimal(value: Double): String = "%.1f".format(value).replace('.', ',')

@Preview(showBackground = true)
@Composable
private fun MeasurementHistoryScreenPreview() {
    NsTheme {
        MeasurementHistoryContent(
            state = MeasurementHistoryUiState(
                childName = "Lucía",
                measurements = listOf(
                    Measurement("1", "child-1", LocalDate.of(2026, 8, 12), 7.8, 68.0, 44.0),
                    Measurement("2", "child-1", LocalDate.of(2026, 7, 14), 7.3, 66.0, 43.0),
                    Measurement("3", "child-1", LocalDate.of(2026, 5, 18), 6.1, 61.0, 41.0),
                ),
            ),
            onBackClick = {},
        )
    }
}
