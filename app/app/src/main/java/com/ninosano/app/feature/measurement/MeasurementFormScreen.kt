package com.ninosano.app.feature.measurement

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ninosano.app.core.designsystem.component.NsButton
import com.ninosano.app.core.designsystem.component.NsButtonVariant
import com.ninosano.app.core.designsystem.component.NsDatePickerField
import com.ninosano.app.core.designsystem.component.NsInputField
import com.ninosano.app.core.designsystem.component.NsTopAppBar
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
import java.time.LocalDate

@Composable
fun MeasurementFormScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: MeasurementFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onSaveSuccess() }
    }

    MeasurementFormContent(
        state = state,
        onBackClick = onBackClick,
        onDateSelected = viewModel::onDateSelected,
        onWeightChange = viewModel::onWeightChange,
        onHeightChange = viewModel::onHeightChange,
        onHeadCircumferenceChange = viewModel::onHeadCircumferenceChange,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun MeasurementFormContent(
    state: MeasurementFormUiState,
    onBackClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onHeadCircumferenceChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NsTopAppBar(title = "Registrar medición", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            NsDatePickerField(
                label = "Fecha",
                selectedDate = state.date,
                onDateSelected = onDateSelected,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            NsInputField(
                label = "Peso (kg)",
                value = state.weightKg,
                onValueChange = onWeightChange,
                placeholder = "Ej: 7,8",
                errorText = state.weightError,
                keyboardType = KeyboardType.Decimal,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            NsInputField(
                label = "Talla / longitud (cm)",
                value = state.heightCm,
                onValueChange = onHeightChange,
                placeholder = "Ej: 68",
                errorText = state.heightError,
                keyboardType = KeyboardType.Decimal,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(16.dp))
            NsInputField(
                label = "Perímetro cefálico (cm)",
                value = state.headCircumferenceCm,
                onValueChange = onHeadCircumferenceChange,
                placeholder = "Ej: 44",
                errorText = state.headCircumferenceError,
                keyboardType = KeyboardType.Decimal,
                enabled = !state.isLoading,
            )

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = NsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
            NsButton(
                text = "Guardar medición",
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
private fun MeasurementFormScreenPreview() {
    NsTheme {
        MeasurementFormContent(
            state = MeasurementFormUiState(weightKg = "7,8", heightCm = "68"),
            onBackClick = {},
            onDateSelected = {},
            onWeightChange = {},
            onHeightChange = {},
            onHeadCircumferenceChange = {},
            onSubmit = {},
        )
    }
}
