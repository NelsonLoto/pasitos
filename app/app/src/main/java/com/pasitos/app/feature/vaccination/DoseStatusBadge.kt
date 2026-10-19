package com.pasitos.app.feature.vaccination

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.shared.vaccination.DoseStatus
import java.time.format.DateTimeFormatter

/** Fechas de vacunación como en el carnet. */
val VaccinationDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** El estado de una dosis con ícono y texto, para que se lea también sin distinguir colores (RF-VAC-01, CA1). */
@Composable
fun DoseStatusBadge(status: DoseStatus, modifier: Modifier = Modifier) {
    PsBadge(text = status.label, tone = status.tone, icon = status.icon, modifier = modifier)
}

val DoseStatus.label: String
    get() = when (this) {
        DoseStatus.APPLIED -> "Aplicada"
        DoseStatus.OVERDUE -> "Atrasada"
        DoseStatus.DUE -> "Pendiente"
        DoseStatus.UPCOMING -> "Próxima"
        DoseStatus.SCHEDULED -> "Programada"
    }

private val DoseStatus.tone: PsBadgeTone
    get() = when (this) {
        DoseStatus.APPLIED -> PsBadgeTone.Success
        DoseStatus.OVERDUE -> PsBadgeTone.Error
        DoseStatus.DUE -> PsBadgeTone.Warning
        DoseStatus.UPCOMING -> PsBadgeTone.Info
        DoseStatus.SCHEDULED -> PsBadgeTone.Neutral
    }

private val DoseStatus.icon: ImageVector
    get() = when (this) {
        DoseStatus.APPLIED -> Icons.Outlined.CheckCircle
        DoseStatus.OVERDUE -> Icons.Outlined.ReportProblem
        DoseStatus.DUE -> Icons.Outlined.HourglassEmpty
        DoseStatus.UPCOMING -> Icons.Outlined.Schedule
        DoseStatus.SCHEDULED -> Icons.Outlined.Event
    }
