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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsDatePickerField
import com.pasitos.app.core.designsystem.component.PsInputField
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.feature.vaccination.DoseStatusBadge
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType
import com.pasitos.shared.vaccination.DoseStatus
import java.time.LocalDate

@Composable
fun ProfessionalGrowthScreen(
    onPatientsClick: () -> Unit,
    viewModel: ProfessionalGrowthViewModel = hiltViewModel(),
) {
    ProfessionalGrowthContent(onPatientsClick = onPatientsClick, today = viewModel.today())
}

@Composable
private fun ProfessionalGrowthContent(onPatientsClick: () -> Unit, today: LocalDate) {
    var date by remember { mutableStateOf(today) }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var headCircumference by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PsTopAppBar(title = "Crecimiento", showBack = false, onBackClick = {})
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            PsCard(Modifier.fillMaxWidth().clickable(onClick = onPatientsClick)) {
                Text("Paciente", style = PsType.Caption, color = PsColor.TextSecondary)
                Text("Seleccionar paciente", style = PsType.BodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                Text("No hay pacientes vinculados a tu cuenta.", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            Text("NUEVA MEDICIÓN", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            PsDatePickerField(label = "Fecha", selectedDate = date, onDateSelected = { date = it }, maxDate = today)
            Spacer(Modifier.height(12.dp))
            PsInputField("Peso (kg)", weight, { weight = it }, placeholder = "Ej: 7,8", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            PsInputField("Talla / longitud (cm)", height, { height = it }, placeholder = "Ej: 68", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            PsInputField("Perímetro cefálico (cm)", headCircumference, { headCircumference = it }, placeholder = "Ej: 44", keyboardType = KeyboardType.Decimal)
            Text("Vinculá un paciente para poder guardar una medición.", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(14.dp))
            PsButton("Registrar medición", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        }
    }
}

@Composable
fun ProfessionalVaccinesScreen(onPatientsClick: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PsTopAppBar(title = "Vacunas", showBack = false, onBackClick = {})
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            PsCard(Modifier.fillMaxWidth().clickable(onClick = onPatientsClick)) {
                Text("Paciente", style = PsType.Caption, color = PsColor.TextSecondary)
                Text("Seleccionar paciente", style = PsType.BodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                Text("Vinculá un paciente para consultar su calendario y el estado de sus vacunas.", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            Text("ESTADOS DE LAS DOSIS", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            PsCard(Modifier.fillMaxWidth()) {
                DoseStatus.entries.forEach { status ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(status.professionalLabel(), style = PsType.BodySmall)
                        DoseStatusBadge(status)
                    }
                }
            }
            Text("La notificación al tutor todavía no está activa.", style = PsType.Caption, color = PsColor.TextSecondary, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(12.dp))
            PsButton("Notificar al tutor por vacuna próxima", onClick = {}, variant = PsButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun DoseStatus.professionalLabel(): String = when (this) {
    DoseStatus.APPLIED -> "Aplicada"
    DoseStatus.OVERDUE -> "Atrasada"
    DoseStatus.DUE -> "Pendiente"
    DoseStatus.UPCOMING -> "Próxima"
    DoseStatus.SCHEDULED -> "Programada"
}

@Preview(showBackground = true)
@Composable
private fun ProfessionalGrowthPreview() {
    PsTheme { ProfessionalGrowthContent(onPatientsClick = {}, today = LocalDate.of(2026, 10, 23)) }
}
