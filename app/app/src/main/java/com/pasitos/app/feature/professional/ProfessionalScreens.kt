package com.pasitos.app.feature.professional

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.ChildAvatar
import com.pasitos.app.core.designsystem.component.childAvatarColor
import com.pasitos.app.data.child.model.Child
import com.pasitos.app.data.child.model.ageAndSexLabel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

@Composable
fun ProfessionalHomeScreen(
    firstName: String,
    onPatientsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAddPatientClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 26.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Hola, $firstName", style = PsType.Caption, color = PsColor.TextSecondary)
                Text("Panel profesional", style = PsType.H2, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificaciones",
                tint = PsColor.TextSecondary,
                modifier = Modifier.clickable(onClick = onNotificationsClick),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfessionalSummaryCard(
                icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = "Pacientes y solicitudes",
                detail = "Consultá los pacientes vinculados y las solicitudes de las familias.",
                action = "Ver pacientes y solicitudes",
                onClick = onPatientsClick,
            )
            Text(
                "Cuando una familia te vincule, sus pacientes aparecerán en esta sección.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            )
            PsButton(
                text = "+ Agregar paciente",
                onClick = onAddPatientClick,
                variant = PsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfessionalSummaryCard(icon: @Composable () -> Unit, title: String, detail: String, action: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Text(title, style = PsType.BodyMedium, modifier = Modifier.padding(start = 10.dp))
        }
        Text(detail, style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 10.dp))
        Text(action, style = PsType.BodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun ProfessionalPatientsScreen(
    onBackClick: () -> Unit,
    onAddPatientClick: () -> Unit = {},
    onPatientClick: (String) -> Unit = {},
    viewModel: ProfessionalPatientsViewModel = hiltViewModel(),
) {
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PsTopAppBar(title = "Pacientes y solicitudes", showBack = true, onBackClick = onBackClick)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PACIENTES", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
            PsButton("+ Agregar paciente", onClick = onAddPatientClick, variant = PsButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
            if (patients.isEmpty()) {
                Column(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)).padding(16.dp)) {
                    Text("Todavía no tenés pacientes", style = PsType.BodyMedium)
                    Text("Agregá un paciente para registrar sus mediciones y vacunas.", style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 6.dp))
                }
            } else {
                patients.forEach { patient ->
                    Row(
                        Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .clickable { onPatientClick(patient.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChildAvatar(patient.avatarKey, patient.firstName.take(1).uppercase(), childAvatarColor(patient.id), size = 46.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("${patient.firstName} ${patient.lastName}", style = PsType.BodyMedium)
                            Text(patient.ageAndSexLabel(viewModel.today()), style = PsType.Caption, color = PsColor.TextSecondary)
                        }
                    }
                }
            }
            Text("SOLICITUDES", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 8.dp))
            Column(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)).padding(16.dp)) {
                Text("No tenés solicitudes pendientes", style = PsType.BodyMedium)
                Text("Las solicitudes de vinculación de las familias aparecerán acá.", style = PsType.BodySmall, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfessionalHomePreview() {
    PsTheme { ProfessionalHomeScreen("Ana", {}, {}) }
}
