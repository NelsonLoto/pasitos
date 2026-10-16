package com.pasitos.app.feature.advicedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.core.designsystem.component.PsBadge
import com.pasitos.app.core.designsystem.component.PsBadgeTone
import com.pasitos.app.core.designsystem.component.PsTopAppBar
import com.pasitos.app.ui.theme.PsColor
import com.pasitos.app.ui.theme.PsTheme
import com.pasitos.app.ui.theme.PsType

private const val ADVICE_SOURCE_DISCLAIMER = "Fuente: Ministerio de Salud de Argentina · Sociedad Argentina de Pediatría (SAP). " +
    "Este contenido no reemplaza el criterio médico profesional."

@Composable
fun AdviceDetailScreen(
    onBackClick: () -> Unit,
    viewModel: AdviceDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdviceDetailContent(state = state, onBackClick = onBackClick)
}

@Composable
private fun AdviceDetailContent(state: AdviceDetailUiState, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PsTopAppBar(title = "Consejo", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PsBadge(text = state.badgeText, tone = PsBadgeTone.Info)
            Text(text = state.title, style = PsType.H2)
            Text(text = state.emoji, fontSize = 40.sp)

            state.body.forEach { paragraph ->
                Text(text = paragraph, style = PsType.BodySmall)
            }

            HorizontalDivider(color = PsColor.Border)
            Text(text = ADVICE_SOURCE_DISCLAIMER, style = PsType.Caption, color = PsColor.TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdviceDetailScreenPreview() {
    PsTheme {
        AdviceDetailContent(
            state = AdviceDetailUiState(
                badgeText = "ALIMENTACIÓN · 6 A 8 MESES",
                title = "Introducción de sólidos",
                emoji = "🥄",
                body = listOf(
                    "A partir de los 6 meses, además de la leche materna o de fórmula, podés empezar a incorporar alimentos semisólidos.",
                    "Introducí un alimento nuevo por vez, con intervalos de 2 a 3 días, para poder identificar posibles alergias.",
                ),
            ),
            onBackClick = {},
        )
    }
}
