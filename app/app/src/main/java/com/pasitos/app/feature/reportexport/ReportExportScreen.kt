package com.pasitos.app.feature.reportexport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.NsButton
import com.pasitos.app.core.designsystem.component.NsButtonVariant
import com.pasitos.app.core.designsystem.component.NsTopAppBar
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

@Composable
fun ReportExportScreen(
    onBackClick: () -> Unit,
    viewModel: ReportExportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReportExportContent(
        state = state,
        onBackClick = onBackClick,
        onToggleGrowthChart = viewModel::onToggleGrowthChart,
        onToggleMeasurements = viewModel::onToggleMeasurements,
        onToggleVaccinationStatus = viewModel::onToggleVaccinationStatus,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun ReportExportContent(
    state: ReportExportUiState,
    onBackClick: () -> Unit,
    onToggleGrowthChart: (Boolean) -> Unit,
    onToggleMeasurements: (Boolean) -> Unit,
    onToggleVaccinationStatus: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NsTopAppBar(title = "Exportar reporte", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ReportPreviewCard(childName = state.childName)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Incluir en el reporte", style = NsType.BodySmall)

                ReportSectionCheckbox(
                    label = "Curva de crecimiento",
                    checked = state.includeGrowthChart,
                    onCheckedChange = onToggleGrowthChart,
                )
                ReportSectionCheckbox(
                    label = "Mediciones registradas",
                    checked = state.includeMeasurements,
                    onCheckedChange = onToggleMeasurements,
                )
                ReportSectionCheckbox(
                    label = "Estado de vacunación",
                    checked = state.includeVaccinationStatus,
                    onCheckedChange = onToggleVaccinationStatus,
                )
            }

            NsButton(
                text = "Generar y compartir PDF",
                onClick = onSubmit,
                variant = NsButtonVariant.Primary,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Se genera un PDF descargable para compartir con el pediatra en la consulta.",
                style = NsType.Caption,
                color = NsColor.TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            state.generalError?.let { error ->
                Text(text = error, style = NsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }
            state.successMessage?.let { message ->
                Text(text = message, style = NsType.BodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ReportPreviewCard(childName: String) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = NsColor.Border, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = if (childName.isNotBlank()) "Reporte — $childName" else "Reporte",
            style = NsType.H3.copy(fontSize = 12.sp),
        )
        ReportPreviewChart()
        SkeletonBar(widthFraction = 1f)
        SkeletonBar(widthFraction = 0.84f)
        SkeletonBar(widthFraction = 0.68f)
    }
}

@Composable
private fun ReportPreviewChart() {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = NsColor.Border
    Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        fun point(xFraction: Float, yFraction: Float) = Offset(size.width * xFraction, size.height * yFraction)

        val dashedPoints = listOf(
            point(2f / 188f, 40f / 60f),
            point(40f / 188f, 34f / 60f),
            point(80f / 188f, 26f / 60f),
            point(186f / 188f, 2f / 60f),
        )
        val tealPoints = listOf(
            point(2f / 188f, 52f / 60f),
            point(40f / 188f, 44f / 60f),
            point(80f / 188f, 34f / 60f),
            point(186f / 188f, 10f / 60f),
        )

        for (i in 0 until dashedPoints.size - 1) {
            drawLine(
                color = borderColor,
                start = dashedPoints[i],
                end = dashedPoints[i + 1],
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
        }
        for (i in 0 until tealPoints.size - 1) {
            drawLine(
                color = primaryColor,
                start = tealPoints[i],
                end = tealPoints[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(6.dp)
            .background(color = NsColor.Border, shape = RoundedCornerShape(3.dp)),
    ) {}
}

@Composable
private fun ReportSectionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = NsColor.TextOnPrimary,
            ),
        )
        Text(text = label, style = NsType.BodySmall, modifier = Modifier.padding(start = 2.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportExportScreenPreview() {
    NsTheme {
        ReportExportContent(
            state = ReportExportUiState(childName = "Lucía"),
            onBackClick = {},
            onToggleGrowthChart = {},
            onToggleMeasurements = {},
            onToggleVaccinationStatus = {},
            onSubmit = {},
        )
    }
}
