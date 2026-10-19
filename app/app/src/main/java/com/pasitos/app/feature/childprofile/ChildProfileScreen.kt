package com.pasitos.app.feature.childprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsListRow
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.core.designsystem.component.childAvatarColor
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ChildSex
import com.pasitos.app.data.child.model.toDecimalText
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ProfileDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ChildProfileScreen(
    onBackClick: () -> Unit,
    onEditClick: (childId: String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ChildProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onDeleted() }
    }

    ChildProfileContent(
        state = state,
        onBackClick = onBackClick,
        onEditClick = { onEditClick(viewModel.childId) },
        onDeleteClick = viewModel::onDeleteClick,
        onTypedNameChange = viewModel::onTypedNameChange,
        onConfirmDeletion = viewModel::onConfirmDeletion,
        onDismissDeletion = viewModel::onDismissDeletion,
    )
}

@Composable
private fun ChildProfileContent(
    state: ChildProfileUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTypedNameChange: (String) -> Unit,
    onConfirmDeletion: () -> Unit,
    onDismissDeletion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Perfil", showBack = true, onBackClick = onBackClick)

        val child = state.child
        if (child == null) {
            if (state.isLoaded) {
                Text(
                    text = "Este perfil ya no está disponible.",
                    style = PsType.BodyRegular,
                    color = PsColor.TextSecondary,
                    modifier = Modifier.padding(24.dp),
                )
            }
            return@Column
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            ChildAvatar(
                avatarKey = child.avatarKey,
                initial = child.firstName.take(1).uppercase(),
                color = childAvatarColor(child.id),
                size = 88.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = child.fullName, style = PsType.H2, textAlign = TextAlign.Center)
            Text(text = state.ageLabel, style = PsType.BodySmall, color = PsColor.TextSecondary, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(20.dp))
            ProfileCard(title = "Datos") {
                PsListRow(label = "Sexo", value = if (child.sex == ChildSex.Female) "Femenino" else "Masculino")
                PsListRow(label = "Fecha de nacimiento", value = child.birthDate.format(ProfileDateFormatter))
                PsListRow(
                    label = "Semanas de gestación",
                    value = child.gestationalWeeks?.toString() ?: "No lo sé",
                    showDivider = false,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProfileCard(title = "Medidas al nacer") {
                PsListRow(label = "Peso", value = child.birthWeightKg.withUnit("kg"))
                PsListRow(label = "Longitud", value = child.birthLengthCm.withUnit("cm"))
                PsListRow(label = "Perímetro cefálico", value = child.birthHeadCircumferenceCm.withUnit("cm"), showDivider = false)
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProfileCard(title = "Salud") {
                HealthText(label = "Alergias", text = child.allergies)
                Spacer(modifier = Modifier.height(12.dp))
                HealthText(label = "Antecedentes relevantes", text = child.relevantHistory)
            }

            if (!state.isOnline) {
                Spacer(modifier = Modifier.height(16.dp))
                AlertCard(
                    text = "Necesitás conexión a internet para editar o eliminar el perfil.",
                    tone = PsAlertTone.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.takeIf { state.deletion == DeletionStep.None }?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(20.dp))
            PsButton(
                text = "Editar",
                onClick = onEditClick,
                variant = PsButtonVariant.Primary,
                enabled = state.isOnline && !state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDeleteClick, enabled = state.isOnline && !state.isWorking) {
                Text(text = "Eliminar perfil", color = if (state.isOnline) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
            }
        }

        when (state.deletion) {
            DeletionStep.None -> Unit
            DeletionStep.ConfirmByName -> DeleteByNameDialog(state, child, onTypedNameChange, onConfirmDeletion, onDismissDeletion)
            DeletionStep.ConfirmLeave -> LeaveDialog(state, child, onConfirmDeletion, onDismissDeletion)
        }
    }
}

/** Único tutor: consecuencias de RN-HIJ-08 y confirmación con el nombre (RF-HIJ-09, CA1). */
@Composable
private fun DeleteByNameDialog(
    state: ChildProfileUiState,
    child: Child,
    onTypedNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "¿Eliminar el perfil de ${child.firstName}?") },
        text = {
            Column {
                Text(
                    text = "Se borra todo su historial: mediciones, vacunas, eventos, hábitos, hitos, invitaciones y " +
                        "vínculos. Las consultas cerradas quedan en el registro del pediatra que las hizo. " +
                        "No se puede deshacer.",
                )
                Spacer(modifier = Modifier.height(12.dp))
                PsInputField(
                    label = "Para confirmar, escribí su nombre",
                    value = state.typedName,
                    onValueChange = onTypedNameChange,
                    placeholder = child.firstName,
                    enabled = !state.isWorking,
                )
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.canConfirmDeletion) {
                Text(text = "Eliminar", color = if (state.canConfirmDeletion) MaterialTheme.colorScheme.error else PsColor.TextSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancelar") }
        },
    )
}

/** Hay otros tutores: el perfil sigue para ellos (RF-HIJ-09, CA2). */
@Composable
private fun LeaveDialog(state: ChildProfileUiState, child: Child, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "¿Dejar de ser tutor de ${child.firstName}?") },
        text = {
            Column {
                Text(
                    text = "El perfil va a seguir existiendo para sus otros tutores y vos vas a dejar de verlo." +
                        if (state.isPrincipal) " Como sos el tutor principal, ese rol pasa al tutor más antiguo." else "",
                )
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, style = PsType.BodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.canConfirmDeletion) {
                Text(text = "Dejar de ser tutor", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancelar") }
        },
    )
}

@Composable
private fun ProfileCard(title: String, content: @Composable () -> Unit) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = PsType.BodyMedium)
        content()
    }
}

@Composable
private fun HealthText(label: String, text: String?) {
    Text(text = label, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
    Text(text = text ?: "Sin datos", style = PsType.BodyRegular, color = if (text == null) PsColor.TextSecondary else PsColor.TextPrimary)
}

private fun Double?.withUnit(unit: String): String = this?.let { "${it.toDecimalText()} $unit" } ?: "Sin datos"

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun ChildProfileScreenPreview() {
    PsTheme {
        ChildProfileContent(
            state = ChildProfileUiState(
                child = Child(
                    id = "child-1",
                    firstName = "Lucía",
                    lastName = "Gómez",
                    sex = ChildSex.Female,
                    birthDate = LocalDate.of(2026, 3, 15),
                    gestationalWeeks = 34,
                    birthWeightKg = 2.35,
                    birthLengthCm = 45.0,
                    allergies = "Polen",
                    avatarKey = "bird",
                ),
                isLoaded = true,
                ageLabel = "7 meses y 4 días · 6 meses y 2 días corregidos",
            ),
            onBackClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onTypedNameChange = {},
            onConfirmDeletion = {},
            onDismissDeletion = {},
        )
    }
}
