package com.ninosano.app.feature.vaccinedetail

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
import com.ninosano.app.core.designsystem.component.NsBadge
import com.ninosano.app.core.designsystem.component.NsBadgeTone
import com.ninosano.app.core.designsystem.component.NsButton
import com.ninosano.app.core.designsystem.component.NsButtonVariant
import com.ninosano.app.core.designsystem.component.NsCard
import com.ninosano.app.core.designsystem.component.NsDatePickerField
import com.ninosano.app.core.designsystem.component.NsInputField
import com.ninosano.app.core.designsystem.component.NsTopAppBar
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
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
        NsTopAppBar(title = state.vaccineDisplayName, showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            NsBadge(text = state.statusText, tone = state.statusTone)

            Spacer(modifier = Modifier.height(16.dp))
            NsCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Edad recomendada: ${state.recommendedAgeLabel}", style = NsType.BodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = state.description, style = NsType.Caption, color = NsColor.TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Registrar aplicación", style = NsType.BodySmall)

            Spacer(modifier = Modifier.height(16.dp))
            NsDatePickerField(
                label = "Fecha de aplicación",
                selectedDate = state.date,
                onDateSelected = onDateSelected,
                errorText = state.dateError,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            NsInputField(
                label = "Lote (opcional)",
                value = state.lot,
                onValueChange = onLotChange,
                placeholder = "Ej: A1234B",
                enabled = !state.isLoading,
            )

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = NsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))
            NsButton(
                text = "Guardar aplicación",
                onClick = onSubmit,
                variant = NsButtonVariant.Primary,
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
    NsTheme {
        VaccineDetailContent(
            state = VaccineDetailUiState(
                vaccineDisplayName = "Pentavalente (3ª dosis)",
                statusText = "Atrasada · Correspondía a los 6 meses",
                statusTone = NsBadgeTone.Error,
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
