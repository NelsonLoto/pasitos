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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate

@Composable
fun VaccineDetailScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: VaccineDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onSaveSuccess() }
    }

    VaccineDetailContent(
        state = state,
        onBackClick = onBackClick,
        onDateSelected = viewModel::onDateSelected,
        onLotChange = viewModel::onLotChange,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun VaccineDetailContent(
    state: VaccineDetailUiState,
    onBackClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onLotChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = state.vaccineDisplayName, showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            PsBadge(text = state.statusText, tone = state.statusTone)

            Spacer(modifier = Modifier.height(16.dp))
            PsCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Edad recomendada: ${state.recommendedAgeLabel}", style = PsType.BodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = state.description, style = PsType.Caption, color = PsColor.TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Registrar aplicación", style = PsType.BodySmall)

            Spacer(modifier = Modifier.height(16.dp))
            PsDatePickerField(
                label = "Fecha de aplicación",
                selectedDate = state.date,
                onDateSelected = onDateSelected,
                errorText = state.dateError,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            PsInputField(
                label = "Lote (opcional)",
                value = state.lot,
                onValueChange = onLotChange,
                placeholder = "Ej: A1234B",
                enabled = !state.isLoading,
            )

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))
            PsButton(
                text = "Guardar aplicación",
                onClick = onSubmit,
                variant = PsButtonVariant.Primary,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VaccineDetailScreenPreview() {
    PsTheme {
        VaccineDetailContent(
            state = VaccineDetailUiState(
                vaccineDisplayName = "Pentavalente (3ª dosis)",
                statusText = "Atrasada · Correspondía a los 6 meses",
                statusTone = PsBadgeTone.Error,
                recommendedAgeLabel = "6 meses",
                description = "Protege contra difteria, tétanos, tos convulsa, hepatitis B y Haemophilus " +
                    "influenzae tipo b (5 en 1). Tercera dosis del esquema primario.",
                date = LocalDate.now(),
            ),
            onBackClick = {},
            onDateSelected = {},
            onLotChange = {},
            onSubmit = {},
        )
    }
}
