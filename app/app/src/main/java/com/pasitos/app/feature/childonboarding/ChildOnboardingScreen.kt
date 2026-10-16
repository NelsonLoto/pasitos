package com.pasitos.app.feature.childonboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate

@Composable
fun ChildOnboardingScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ChildOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onSaveSuccess() }
    }

    ChildOnboardingContent(
        state = state,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onBirthDateSelected = viewModel::onBirthDateSelected,
        onSexSelected = viewModel::onSexSelected,
        onSubmit = viewModel::onSubmit,
    )
}

@Composable
private fun ChildOnboardingContent(
    state: ChildOnboardingUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onBirthDateSelected: (LocalDate) -> Unit,
    onSexSelected: (ChildSex) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Agregar hijo/a", showBack = true, onBackClick = onBackClick)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = "Contanos un poco sobre tu hijo/a para empezar su seguimiento.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))
            // Solo visual por ahora: no hay carga de foto real (no está en el alcance de RF02
            // ni hay backend/almacenamiento todavía), igual que el diseño de Figma la muestra.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(width = 1.5.dp, color = PsColor.Border, shape = CircleShape),
            )

            Spacer(modifier = Modifier.height(18.dp))
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))
            PsInputField(
                label = "Nombre",
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "Ej: Lucía",
                errorText = state.nameError,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(18.dp))
            PsDatePickerField(
                label = "Fecha de nacimiento",
                selectedDate = state.birthDate,
                onDateSelected = onBirthDateSelected,
                errorText = state.birthDateError,
                enabled = !state.isLoading,
            )

            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "Sexo", style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SexOption(
                    label = "Niña",
                    selected = state.sex == ChildSex.Female,
                    onClick = { onSexSelected(ChildSex.Female) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                SexOption(
                    label = "Niño",
                    selected = state.sex == ChildSex.Male,
                    onClick = { onSexSelected(ChildSex.Male) },
                    modifier = Modifier.weight(1f),
                )
            }

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(18.dp))
            PsButton(
                text = "Guardar y continuar",
                onClick = onSubmit,
                variant = PsButtonVariant.Primary,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SexOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
                } else {
                    Modifier
                        .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                        .border(width = 1.5.dp, color = PsColor.Border, shape = RoundedCornerShape(10.dp))
                },
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = PsType.BodyRegular,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else PsColor.TextPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChildOnboardingScreenPreview() {
    PsTheme {
        ChildOnboardingContent(
            state = ChildOnboardingUiState(name = "Lucía", birthDate = LocalDate.of(2024, 3, 15)),
            onBackClick = {},
            onNameChange = {},
            onBirthDateSelected = {},
            onSexSelected = {},
            onSubmit = {},
        )
    }
}
