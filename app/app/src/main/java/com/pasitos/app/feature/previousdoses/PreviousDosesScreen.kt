package com.pasitos.app.feature.previousdoses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun PreviousDosesScreen(
    onFinished: () -> Unit,
    viewModel: PreviousDosesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.finished.collect { onFinished() }
    }
    // Después del alta es la única pantalla de la pila: "atrás" lleva a Inicio en vez de cerrar la app
    BackHandler(enabled = state.afterChildCreated) { viewModel.onFinish() }

    PreviousDosesContent(
        state = state,
        onBackClick = viewModel::onFinish,
        onAnswer = viewModel::onAnswer,
        onToggle = viewModel::onToggle,
        onSave = viewModel::onSave,
        onFinish = viewModel::onFinish,
    )
}

@Composable
private fun PreviousDosesContent(
    state: PreviousDosesUiState,
    onBackClick: () -> Unit,
    onAnswer: (upToDate: Boolean) -> Unit,
    onToggle: (doseCode: String, checked: Boolean) -> Unit,
    onSave: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Dosis anteriores", showBack = !state.afterChildCreated, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            if (!state.isLoaded) return@Column
            when {
                state.step == PreviousDosesStep.Done -> DoneStep(state, onFinish)
                state.total == 0 -> NothingToMark(state, onFinish)
                state.step == PreviousDosesStep.Question -> QuestionStep(state, onAnswer, onFinish)
                else -> ChecklistStep(state, onToggle, onSave)
            }
        }
    }
}

@Composable
private fun QuestionStep(state: PreviousDosesUiState, onAnswer: (Boolean) -> Unit, onSkip: () -> Unit) {
    Text(
        text = "Según el Calendario Nacional, a su edad ${state.childName} ya debería tener ${state.total} " +
            (if (state.total == 1) "dosis aplicada." else "dosis aplicadas."),
        style = PsType.BodyRegular,
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text(text = "¿Tiene las vacunas al día para su edad?", style = PsType.H3)
    Spacer(modifier = Modifier.height(20.dp))
    PsButton(text = "Sí", onClick = { onAnswer(true) }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    PsButton(text = "No", onClick = { onAnswer(false) }, variant = PsButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    PsButton(text = "No sé", onClick = { onAnswer(false) }, variant = PsButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    if (state.afterChildCreated) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text(text = "Ahora no") }
    }
}

@Composable
private fun ChecklistStep(state: PreviousDosesUiState, onToggle: (String, Boolean) -> Unit, onSave: () -> Unit) {
    Text(
        text = "Marcá las dosis que se aplicaron. Quedan con fecha no registrada; después podés completar " +
            "la fecha, el lote y el lugar desde cada dosis.",
        style = PsType.BodySmall,
        color = PsColor.TextSecondary,
    )
    state.groups.forEach { group ->
        Text(text = group.label, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        PsCard(modifier = Modifier.fillMaxWidth()) {
            group.doses.forEach { dose ->
                val checked = dose.doseCode in state.checked
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = checked, enabled = !state.isSaving, role = Role.Checkbox) { onToggle(dose.doseCode, it) }
                        .padding(vertical = 2.dp),
                ) {
                    Checkbox(checked = checked, onCheckedChange = null, enabled = !state.isSaving)
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = dose.vaccineName, style = PsType.BodySmall)
                        Text(text = dose.doseLabel, style = PsType.Caption, color = PsColor.TextSecondary)
                    }
                }
            }
        }
    }

    if (!state.isOnline) {
        Spacer(modifier = Modifier.height(16.dp))
        AlertCard(text = "Necesitás conexión a internet para guardar las dosis.", tone = PsAlertTone.Warning, modifier = Modifier.fillMaxWidth())
    }
    state.error?.let {
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = it, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
    }
    Spacer(modifier = Modifier.height(20.dp))
    PsButton(
        text = if (state.checked.isEmpty()) "Guardar" else "Guardar ${state.checked.size} dosis",
        onClick = onSave,
        enabled = state.canSave,
        isLoading = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DoneStep(state: PreviousDosesUiState, onFinish: () -> Unit) {
    Text(
        text = if (state.savedCount == 1) "Registramos 1 dosis." else "Registramos ${state.savedCount} dosis.",
        style = PsType.H3,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Podés completar la fecha, el lote y el lugar desde cada dosis, en Vacunas.",
        style = PsType.BodyRegular,
        color = PsColor.TextSecondary,
    )
    Spacer(modifier = Modifier.height(20.dp))
    PsButton(text = "Continuar", onClick = onFinish, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NothingToMark(state: PreviousDosesUiState, onFinish: () -> Unit) {
    Text(
        text = "${state.childName} no tiene dosis anteriores para marcar.".trim(),
        style = PsType.BodyRegular,
    )
    Spacer(modifier = Modifier.height(20.dp))
    PsButton(text = "Continuar", onClick = onFinish, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
private fun PreviousDosesChecklistPreview() {
    PsTheme {
        PreviousDosesContent(
            state = PreviousDosesUiState(
                isLoaded = true,
                childName = "Lucía",
                step = PreviousDosesStep.Checklist,
                groups = listOf(
                    PreviousDoseGroupUi("Al nacer", listOf(PreviousDoseUi("BCG", "BCG", "Única dosis"), PreviousDoseUi("HB-NEO", "Hepatitis B", "Dosis neonatal"))),
                    PreviousDoseGroupUi("2 meses", listOf(PreviousDoseUi("NEUMO-1", "Neumococo conjugada", "1ª dosis"))),
                ),
                checked = setOf("BCG", "HB-NEO"),
            ),
            onBackClick = {},
            onAnswer = {},
            onToggle = { _, _ -> },
            onSave = {},
            onFinish = {},
        )
    }
}
