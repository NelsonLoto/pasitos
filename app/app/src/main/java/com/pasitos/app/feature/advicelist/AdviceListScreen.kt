package com.pasitos.app.feature.advicelist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.NsCard
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType

@Composable
fun AdviceListScreen(
    onChildChipClick: () -> Unit,
    onAdviceClick: (adviceId: String) -> Unit,
    viewModel: AdviceListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AdviceListContent(state = state, onChildChipClick = onChildChipClick, onAdviceClick = onAdviceClick)
}

@Composable
private fun AdviceListContent(
    state: AdviceListUiState,
    onChildChipClick: () -> Unit,
    onAdviceClick: (adviceId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 30.dp, bottom = 14.dp),
        ) {
            Text(text = "Consejos", style = NsType.H3)
            if (state.childName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onChildChipClick),
                ) {
                    Text(text = state.childName, style = NsType.BodySmall)
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = null,
                        tint = NsColor.TextPrimary,
                        modifier = Modifier
                            .padding(start = 3.dp)
                            .size(14.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = state.stageLabel, style = NsType.H3.copy(fontSize = 15.sp))

            state.tips.forEachIndexed { index, tip ->
                AdviceTipRow(tip = tip, useAccentTone = index % 2 == 1, onClick = { onAdviceClick(tip.id) })
            }
        }
    }
}

@Composable
private fun AdviceTipRow(tip: AdviceTipUiModel, useAccentTone: Boolean, onClick: () -> Unit) {
    val squareColor = if (useAccentTone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    NsCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .size(42.dp)
                    .background(color = squareColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = tip.emoji, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
            }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = tip.category,
                    style = NsType.Caption.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, fontSize = 10.5.sp),
                    color = NsColor.TextSecondary,
                )
                Text(text = tip.title, style = NsType.BodySmall, modifier = Modifier.padding(top = 1.dp))
                Text(
                    text = tip.summary,
                    style = NsType.Caption,
                    color = NsColor.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = NsColor.TextSecondary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdviceListScreenPreview() {
    NsTheme {
        AdviceListContent(
            state = AdviceListUiState(
                childName = "Lucía",
                stageLabel = "¿Qué se viene? · 8 meses",
                tips = listOf(
                    AdviceTipUiModel("1", "ALIMENTACIÓN", "Introducción de sólidos", "Cómo empezar con papillas y purés a partir de los 6 meses.", "🥄"),
                    AdviceTipUiModel("2", "DESARROLLO MOTOR", "Sentado sin apoyo", "Qué esperar en el control de los 8 meses.", "🚼"),
                ),
            ),
            onChildChipClick = {},
            onAdviceClick = {},
        )
    }
}
