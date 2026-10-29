package com.pasitos.app.feature.growthchart.proposal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pasitos.app.core.designsystem.component.AlertCard
import com.pasitos.app.core.designsystem.component.PsAlertTone
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsButton
import com.pasitos.app.core.designsystem.component.PsButtonVariant
import com.pasitos.app.core.designsystem.component.PsCard
import com.pasitos.app.core.designsystem.component.PsSegmentedControl
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

/** Ejemplos de presentación. No son estados de dominio ni resultados de un cálculo. */
private enum class GrowthStatusExample(
    val tab: String,
    val title: String,
    val message: String,
    val tone: PsBadgeTone,
    val icon: ImageVector,
) {
    EXPECTED(
        tab = "Esperado",
        title = "Dentro de lo esperado",
        message = "El resultado de este indicador está dentro de los criterios de referencia. Podés ver su evolución en el historial.",
        tone = PsBadgeTone.Success,
        icon = Icons.Outlined.CheckCircle,
    ),
    FOLLOW_UP(
        tab = "A controlar",
        title = "A controlar",
        message = "La evolución de este indicador requiere seguimiento. Revisá el historial y compartilo con tu pediatra.",
        tone = PsBadgeTone.Warning,
        icon = Icons.Outlined.Info,
    ),
    CONSULT(
        tab = "Consultar",
        title = "Consultar con el pediatra",
        message = "Este resultado requiere una revisión con tu pediatra. Podés compartirle las mediciones registradas.",
        tone = PsBadgeTone.Error,
        icon = Icons.Outlined.MedicalServices,
    ),
}

@Composable
internal fun GrowthStatusProposalScreen(onBack: () -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val example = GrowthStatusExample.entries[selectedIndex]
    // El control segmentado actual es compacto. Con texto ampliado se usan los botones
    // existentes en columna para conservar etiquetas completas y blancos de toque amplios.
    val useStackedSelector = LocalDensity.current.fontScale > 1.3f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        PsTopAppBar(title = "Propuesta de estados", showBack = true, onBackClick = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            AlertCard(
                text = "Demostración visual. Estados y textos de ejemplo, pendientes de validación. No evalúa mediciones.",
                tone = PsAlertTone.Info,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Elegí un ejemplo", style = PsType.BodyMedium)
            if (useStackedSelector) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GrowthStatusExample.entries.forEachIndexed { index, option ->
                        PsButton(
                            text = option.tab,
                            onClick = { selectedIndex = index },
                            variant = if (selectedIndex == index) PsButtonVariant.Primary else PsButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth().semantics { selected = selectedIndex == index },
                        )
                    }
                }
            } else {
                PsSegmentedControl(
                    options = GrowthStatusExample.entries.map { it.tab },
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GrowthStatusExampleCard(example)
            Text(
                text = "Esta tarjeta es una propuesta para la sección Crecimiento. La pantalla habitual conserva el aviso de evaluación pendiente hasta contar con el motor de I2.",
                style = PsType.Caption,
                color = PsColor.TextSecondary,
            )
        }
    }
}

@Composable
private fun GrowthStatusExampleCard(example: GrowthStatusExample) {
    PsCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ejemplo: peso para la edad", style = PsType.BodyMedium)
            PsBadge(text = example.title, tone = example.tone, icon = example.icon)
            Text(example.message, style = PsType.BodyRegular)
            Text(
                text = "Medición de ejemplo: 28/10/2026",
                style = PsType.Caption,
                color = PsColor.TextSecondary,
            )
            Text(
                text = "Este estado orienta, no diagnostica. Ante cualquier duda, consultá con tu pediatra.",
                style = PsType.BodySmall,
                color = PsColor.TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GrowthStatusProposalPreview() {
    PsTheme {
        GrowthStatusProposalScreen(onBack = {})
    }
}
