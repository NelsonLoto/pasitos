package com.pasitos.app.feature.growthchart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsLineChart
import com.pasitos.app.core.designsystem.component.PsSegmentedControl
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun GrowthChartScreen(
    onChildChipClick: () -> Unit,
    onViewHistoryClick: (childId: String) -> Unit,
    onRegisterMeasurementClick: (childId: String) -> Unit,
    viewModel: GrowthChartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GrowthChartContent(
        childId = viewModel.childId,
        state = state,
        onChildChipClick = onChildChipClick,
        onMetricSelected = viewModel::onMetricSelected,
        onViewHistoryClick = onViewHistoryClick,
        onRegisterMeasurementClick = onRegisterMeasurementClick,
    )
}

@Composable
private fun GrowthChartContent(
    childId: String,
    state: GrowthChartUiState,
    onChildChipClick: () -> Unit,
    onMetricSelected: (MeasurementMetric) -> Unit,
    onViewHistoryClick: (childId: String) -> Unit,
    onRegisterMeasurementClick: (childId: String) -> Unit,
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
            Text(text = "Crecimiento", style = PsType.H3)
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

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            PsSegmentedControl(
                options = MeasurementMetric.entries.map { it.label },
                selectedIndex = MeasurementMetric.entries.indexOf(state.selectedMetric),
                onSelect = { onMetricSelected(MeasurementMetric.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))
            PsCard(modifier = Modifier.fillMaxWidth()) {
                PsLineChart(values = state.points.map { it.value })
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "El cálculo de percentil está pendiente de validación médica (RN02/RN03) y todavía no se muestra.",
                style = PsType.Caption,
                color = PsColor.TextSecondary,
            )

            Spacer(modifier = Modifier.height(14.dp))
            val lastPoint = state.points.lastOrNull()
            if (lastPoint != null) {
                Row {
                    Text(
                        text = "Última medición: ${formatValue(lastPoint.value)} ${state.selectedMetric.unit} el ${lastPoint.date.format(DateFormatter)} · ",
                        style = PsType.BodySmall,
                        color = PsColor.TextSecondary,
                    )
                    Text(
                        text = "Ver historial →",
                        style = PsType.BodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onViewHistoryClick(childId) },
                    )
                }
            } else {
                Row {
                    Text(
                        text = "Todavía no cargaste ${state.selectedMetric.label.lowercase()}" + if (state.canAdd) " · " else ".",
                        style = PsType.BodySmall,
                        color = PsColor.TextSecondary,
                    )
                    if (state.canAdd) {
                        Text(
                            text = "Registrar medición →",
                            style = PsType.BodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onRegisterMeasurementClick(childId) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GrowthChartScreenPreview() {
    PsTheme {
        GrowthChartContent(
            childId = "child-1",
            state = GrowthChartUiState(
                childName = "Lucía",
                points = listOf(
                    ChartPoint(LocalDate.now().minusMonths(2), 6.2),
                    ChartPoint(LocalDate.now().minusMonths(1), 7.1),
                    ChartPoint(LocalDate.now(), 7.8),
                ),
            ),
            onChildChipClick = {},
            onMetricSelected = {},
            onViewHistoryClick = {},
            onRegisterMeasurementClick = {},
        )
    }
}

private fun formatValue(value: Double): String {
    val rounded = "%.1f".format(value)
    return rounded.replace('.', ',')
}
