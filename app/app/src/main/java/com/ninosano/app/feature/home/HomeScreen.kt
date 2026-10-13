package com.ninosano.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ninosano.app.R
import com.ninosano.app.core.designsystem.component.ChildSelectorChip
import com.ninosano.app.core.designsystem.component.NsButton
import com.ninosano.app.core.designsystem.component.NsButtonVariant
import com.ninosano.app.core.designsystem.component.StatCard
import com.ninosano.app.core.designsystem.component.childAvatarColor
import com.ninosano.app.data.child.model.Child
import com.ninosano.app.data.child.model.ChildSex
import com.ninosano.app.data.child.model.ageAndSexLabel
import com.ninosano.app.data.child.model.ageLabel
import com.ninosano.app.data.measurement.model.Measurement
import com.ninosano.app.ui.theme.NsColor
import com.ninosano.app.ui.theme.NsTheme
import com.ninosano.app.ui.theme.NsType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val HomeDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun HomeScreen(
    onChildChipClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMeasurementClick: (childId: String) -> Unit,
    onVaccinesClick: () -> Unit,
    onAdviceClick: () -> Unit,
    onAddChildClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onChildChipClick = onChildChipClick,
        onNotificationsClick = onNotificationsClick,
        onMeasurementClick = onMeasurementClick,
        onVaccinesClick = onVaccinesClick,
        onAdviceClick = onAdviceClick,
        onAddChildClick = onAddChildClick,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onChildChipClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMeasurementClick: (childId: String) -> Unit,
    onVaccinesClick: () -> Unit,
    onAdviceClick: () -> Unit,
    onAddChildClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Hola, ${state.greetingName}", style = NsType.Caption, color = NsColor.TextSecondary)
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = "Notificaciones",
                    tint = NsColor.TextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onNotificationsClick),
                )
            }

            val child = state.activeChild
            if (child != null) {
                ChildSelectorChip(
                    name = child.name,
                    ageAndSex = child.ageAndSexLabel(),
                    avatarColor = childAvatarColor(child.id),
                    onClick = onChildChipClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                val lastMeasurement = state.lastMeasurement
                if (lastMeasurement != null && lastMeasurement.weightKg != null) {
                    StatCard(
                        icon = Icons.Filled.Scale,
                        label = "ÚLTIMO PESO",
                        value = "${"%.1f".format(lastMeasurement.weightKg)} kg · ${lastMeasurement.date.format(HomeDateFormatter)}",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { state.activeChild?.let { onMeasurementClick(it.id) } },
                    )
                } else {
                    StatCardEmpty(
                        label = "ÚLTIMO PESO",
                        message = "Todavía no registraste mediciones.",
                        actionLabel = "Registrar medición →",
                        onClick = { state.activeChild?.let { onMeasurementClick(it.id) } },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                StatCardEmpty(
                    label = "VACUNAS",
                    message = "Sin calendario cargado.",
                    actionLabel = "Ver calendario →",
                    onClick = onVaccinesClick,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAdviceClick)
                    .background(color = Color(0xFFFBE5D8), shape = RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                val stageLabel = state.activeChild?.let { "Consejos para esta etapa · ${it.ageLabel()}" } ?: "Consejos por edad"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌱", fontSize = 15.sp)
                    Text(text = stageLabel, style = NsType.BodySmall, modifier = Modifier.padding(start = 6.dp))
                }
                Text(
                    text = "Todavía no cargamos contenido de consejos para esta edad.",
                    style = NsType.Caption,
                    color = NsColor.TextSecondary,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    text = "Ver todos los consejos →",
                    style = NsType.Caption,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }

            if (state.activeChild == null) {
                Spacer(modifier = Modifier.height(4.dp))
                NsButton(
                    text = "+ Agregar hijo/a",
                    onClick = onAddChildClick,
                    variant = NsButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatCardEmpty(
    label: String,
    message: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
            .background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp))
            .padding(13.dp),
    ) {
        Text(text = label, style = NsType.Caption, color = NsColor.TextSecondary)
        Text(
            text = message,
            style = NsType.BodySmall,
            color = NsColor.TextSecondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
        Text(text = actionLabel, style = NsType.Caption, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NsTheme {
        HomeContent(
            state = HomeUiState(
                greetingName = "María",
                activeChild = Child(id = "child-1", name = "Lucía", birthDate = LocalDate.now().minusMonths(8), sex = ChildSex.Female),
            ),
            onChildChipClick = {},
            onNotificationsClick = {},
            onMeasurementClick = { _ -> },
            onVaccinesClick = {},
            onAdviceClick = {},
            onAddChildClick = {},
        )
    }
}
