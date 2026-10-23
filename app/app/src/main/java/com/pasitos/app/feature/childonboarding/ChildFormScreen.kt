package com.pasitos.app.feature.childonboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.ChildAvatar
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.core.designsystem.component.avatarLabel
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.api.child.AVATAR_KEYS
import java.time.LocalDate

@Composable
fun ChildFormScreen(
    onBackClick: () -> Unit,
    onSaved: (SavedChild) -> Unit,
    viewModel: ChildFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved(it) }
    }

    ChildFormContent(
        state = state,
        onBackClick = onBackClick,
        actions = ChildFormActions(
            onFirstNameChange = viewModel::onFirstNameChange,
            onLastNameChange = viewModel::onLastNameChange,
            onSexSelected = viewModel::onSexSelected,
            onBirthDateSelected = viewModel::onBirthDateSelected,
            onGestationalWeeksChange = viewModel::onGestationalWeeksChange,
            onGestationalWeeksUnknownChange = viewModel::onGestationalWeeksUnknownChange,
            onBirthWeightChange = viewModel::onBirthWeightChange,
            onBirthLengthChange = viewModel::onBirthLengthChange,
            onBirthHeadCircumferenceChange = viewModel::onBirthHeadCircumferenceChange,
            onAvatarSelected = viewModel::onAvatarSelected,
            onAllergiesChange = viewModel::onAllergiesChange,
            onRelevantHistoryChange = viewModel::onRelevantHistoryChange,
            onRepresentationDeclaredChange = viewModel::onRepresentationDeclaredChange,
            onSubmit = viewModel::onSubmit,
            onConfirmMeasurements = viewModel::onConfirmMeasurements,
            onReviewMeasurements = viewModel::onReviewMeasurements,
            onConfirmRecalculation = viewModel::onConfirmRecalculation,
            onDismissRecalculation = viewModel::onDismissRecalculation,
        ),
    )
}

/** Eventos del formulario; los valores por defecto sirven para las vistas previas. */
private class ChildFormActions(
    val onFirstNameChange: (String) -> Unit = {},
    val onLastNameChange: (String) -> Unit = {},
    val onSexSelected: (ChildSex) -> Unit = {},
    val onBirthDateSelected: (LocalDate) -> Unit = {},
    val onGestationalWeeksChange: (String) -> Unit = {},
    val onGestationalWeeksUnknownChange: (Boolean) -> Unit = {},
    val onBirthWeightChange: (String) -> Unit = {},
    val onBirthLengthChange: (String) -> Unit = {},
    val onBirthHeadCircumferenceChange: (String) -> Unit = {},
    val onAvatarSelected: (String?) -> Unit = {},
    val onAllergiesChange: (String) -> Unit = {},
    val onRelevantHistoryChange: (String) -> Unit = {},
    val onRepresentationDeclaredChange: (Boolean) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onConfirmMeasurements: () -> Unit = {},
    val onReviewMeasurements: () -> Unit = {},
    val onConfirmRecalculation: () -> Unit = {},
    val onDismissRecalculation: () -> Unit = {},
)

@Composable
private fun ChildFormContent(state: ChildFormUiState, onBackClick: () -> Unit, actions: ChildFormActions) {
    val enabled = !state.isSaving && !state.isLoading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = if (state.isEdit) "Editar perfil" else if (state.professionalPatient) "Agregar paciente" else "Agregar hijo/a", showBack = true, onBackClick = onBackClick)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            if (!state.isEdit) {
                Text(
                    text = if (state.professionalPatient) "Completá los datos para empezar el seguimiento de tu paciente." else "Contanos un poco sobre tu hijo/a para empezar su seguimiento.",
                    style = PsType.BodySmall,
                    color = PsColor.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            AvatarPicker(
                selected = state.avatarKey,
                initial = state.firstName.trim().take(1).uppercase(),
                enabled = enabled,
                onSelect = actions.onAvatarSelected,
            )

            Spacer(modifier = Modifier.height(18.dp))
            PsInputField(
                label = "Nombre",
                value = state.firstName,
                onValueChange = actions.onFirstNameChange,
                placeholder = "Ej: Lucía",
                errorText = state.errors.firstName,
                enabled = enabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PsInputField(
                label = "Apellido",
                value = state.lastName,
                onValueChange = actions.onLastNameChange,
                placeholder = "Ej: Gómez",
                errorText = state.errors.lastName,
                enabled = enabled,
            )

            Spacer(modifier = Modifier.height(12.dp))
            PsDatePickerField(
                label = "Fecha de nacimiento",
                selectedDate = state.birthDate,
                onDateSelected = actions.onBirthDateSelected,
                errorText = state.errors.birthDate,
                enabled = enabled,
                maxDate = state.today,
                minDate = state.minBirthDate,
            )

            Spacer(modifier = Modifier.height(18.dp))
            FieldLabel("Sexo")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SexOption(
                    label = "Niña",
                    selected = state.sex == ChildSex.Female,
                    enabled = enabled,
                    onClick = { actions.onSexSelected(ChildSex.Female) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                SexOption(
                    label = "Niño",
                    selected = state.sex == ChildSex.Male,
                    enabled = enabled,
                    onClick = { actions.onSexSelected(ChildSex.Male) },
                    modifier = Modifier.weight(1f),
                )
            }
            state.errors.sex?.let { ErrorText(it) }

            Spacer(modifier = Modifier.height(18.dp))
            PsInputField(
                label = "Semanas de gestación al nacer",
                value = state.gestationalWeeks,
                onValueChange = actions.onGestationalWeeksChange,
                placeholder = "Ej: 39",
                errorText = state.errors.gestationalWeeks,
                keyboardType = KeyboardType.Number,
                enabled = enabled && !state.gestationalWeeksUnknown,
            )
            CheckboxRow(
                label = "No lo sé",
                checked = state.gestationalWeeksUnknown,
                enabled = enabled,
                onCheckedChange = actions.onGestationalWeeksUnknownChange,
            )

            Spacer(modifier = Modifier.height(12.dp))
            SectionTitle("Medidas al nacer (opcional)")
            Text(
                text = "Te recomendamos cargarlas: permiten seguir su crecimiento desde el primer día.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            MeasurementField("Peso (kg)", state.birthWeightKg, "Ej: 3,2", state.errors.birthWeightKg, enabled, actions.onBirthWeightChange)
            Spacer(modifier = Modifier.height(12.dp))
            MeasurementField("Longitud (cm)", state.birthLengthCm, "Ej: 50", state.errors.birthLengthCm, enabled, actions.onBirthLengthChange)
            Spacer(modifier = Modifier.height(12.dp))
            MeasurementField(
                "Perímetro cefálico (cm)",
                state.birthHeadCircumferenceCm,
                "Ej: 34",
                state.errors.birthHeadCircumferenceCm,
                enabled,
                actions.onBirthHeadCircumferenceChange,
            )

            if (state.isEdit) {
                Spacer(modifier = Modifier.height(18.dp))
                SectionTitle("Salud (opcional)")
                LimitedTextField("Alergias", state.allergies, state.errors.allergies, enabled, actions.onAllergiesChange)
                Spacer(modifier = Modifier.height(12.dp))
                LimitedTextField(
                    "Antecedentes relevantes",
                    state.relevantHistory,
                    state.errors.relevantHistory,
                    enabled,
                    actions.onRelevantHistoryChange,
                )
            } else if (!state.professionalPatient) {
                Spacer(modifier = Modifier.height(18.dp))
                CheckboxRow(
                    label = "Soy madre, padre o representante legal de este niño o niña",
                    checked = state.representationDeclared,
                    enabled = enabled,
                    onCheckedChange = actions.onRepresentationDeclaredChange,
                )
                state.errors.representation?.let { ErrorText(it) }
            }

            if (!state.isOnline) {
                Spacer(modifier = Modifier.height(12.dp))
                AlertCard(
                    text = "Necesitás conexión a internet para guardar el perfil.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.generalError?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(18.dp))
            PsButton(
                text = if (state.isEdit) "Guardar cambios" else "Guardar y continuar",
                onClick = actions.onSubmit,
                variant = PsButtonVariant.Primary,
                enabled = enabled && state.isOnline,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.implausibleMeasurements.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = actions.onReviewMeasurements,
            title = { Text(text = "Revisá las medidas al nacer") },
            text = {
                Text(
                    text = "Estos valores son poco habituales:\n" +
                        state.implausibleMeasurements.joinToString("\n") { "• $it" } +
                        "\n\n¿Están bien?",
                )
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmMeasurements) { Text(text = "Sí, están bien") }
            },
            dismissButton = {
                TextButton(onClick = actions.onReviewMeasurements) { Text(text = "Revisar") }
            },
        )
    }

    if (state.confirmRecalculation) {
        AlertDialog(
            onDismissRequest = actions.onDismissRecalculation,
            title = { Text(text = "¿Guardar los cambios?") },
            text = {
                Text(
                    text = "Cambiaste la fecha de nacimiento, el sexo o las semanas de gestación. " +
                        "Van a cambiar sus percentiles, sus estados y su calendario de vacunas.",
                )
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmRecalculation) { Text(text = "Guardar") }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissRecalculation) { Text(text = "Cancelar") }
            },
        )
    }
}

/** Avatares predefinidos y la opción sin avatar, que muestra la inicial (RN-HIJ-11). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarPicker(selected: String?, initial: String, enabled: Boolean, onSelect: (String?) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ChildAvatar(avatarKey = selected, initial = initial, color = PsColor.Primary, size = 88.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Elegí un avatar", style = PsType.BodySmall, color = PsColor.TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarOption(label = "Sin avatar", selected = selected == null, enabled = enabled, onClick = { onSelect(null) }) {
                ChildAvatar(initial = initial, color = PsColor.Primary, size = 40.dp)
            }
            AVATAR_KEYS.forEach { key ->
                AvatarOption(label = avatarLabel(key) ?: key, selected = selected == key, enabled = enabled, onClick = { onSelect(key) }) {
                    ChildAvatar(avatarKey = key, initial = initial, color = PsColor.Primary, size = 40.dp)
                }
            }
        }
    }
}

@Composable
private fun AvatarOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(width = 2.dp, color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Avatar: $label" }
            .padding(3.dp),
    ) {
        content()
    }
}

@Composable
private fun SexOption(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
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

@Composable
private fun CheckboxRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(text = label, style = PsType.BodyRegular, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    PsInputField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        errorText = error,
        keyboardType = KeyboardType.Decimal,
        enabled = enabled,
    )
}

/** Texto breve con su contador de caracteres (RN-HIJ-10). */
@Composable
private fun LimitedTextField(label: String, value: String, error: String?, enabled: Boolean, onValueChange: (String) -> Unit) {
    PsInputField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        errorText = error,
        enabled = enabled,
        singleLine = false,
    )
    Text(
        text = "${value.trim().length}/$MAX_TEXT_LENGTH",
        style = PsType.Caption,
        color = if (value.trim().length > MAX_TEXT_LENGTH) MaterialTheme.colorScheme.error else PsColor.TextSecondary,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = PsType.BodyMedium, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = PsType.Caption,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ChildFormScreenPreview() {
    PsTheme {
        ChildFormContent(
            state = ChildFormUiState(
                firstName = "Lucía",
                lastName = "Gómez",
                sex = ChildSex.Female,
                birthDate = LocalDate.of(2026, 3, 15),
                gestationalWeeks = "39",
                avatarKey = "rabbit",
                today = LocalDate.of(2026, 10, 19),
                minBirthDate = LocalDate.of(2011, 10, 20),
            ),
            onBackClick = {},
            actions = ChildFormActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ChildFormEditPreview() {
    PsTheme {
        ChildFormContent(
            state = ChildFormUiState(
                childId = "child-1",
                firstName = "Lucía",
                lastName = "Gómez",
                sex = ChildSex.Female,
                birthDate = LocalDate.of(2026, 3, 15),
                gestationalWeeksUnknown = true,
                birthWeightKg = "3,2",
                allergies = "Polen",
                isOnline = false,
                today = LocalDate.of(2026, 10, 19),
                minBirthDate = LocalDate.of(2011, 10, 20),
            ),
            onBackClick = {},
            actions = ChildFormActions(),
        )
    }
}
