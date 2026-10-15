package com.pasitos.app.feature.notificationcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pasitos.app.R
import com.pasitos.app.core.designsystem.component.NsCard
import com.pasitos.app.core.designsystem.component.NsTopAppBar
import com.pasitos.app.data.notification.model.NotificationType
import com.pasitos.app.ui.theme.NsColor
import com.pasitos.app.ui.theme.NsTheme
import com.pasitos.app.ui.theme.NsType
import com.pasitos.app.ui.theme.extendedColors

@Composable
fun NotificationCenterScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationCenterContent(state = state, onBackClick = onBackClick)
}

@Composable
private fun NotificationCenterContent(state: NotificationCenterUiState, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NsTopAppBar(title = "Notificaciones", showBack = true, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = section.header,
                        style = NsType.Caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
                        color = NsColor.TextSecondary,
                    )
                    section.items.forEach { item ->
                        NotificationRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItemUiModel) {
    NsCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = iconBackgroundColor(item.type), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes(item.type)),
                    contentDescription = null,
                    tint = NsColor.TextOnPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(text = item.title, style = NsType.BodySmall)
                Text(
                    text = item.body,
                    style = NsType.Caption,
                    color = NsColor.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = item.relativeTime,
                    style = NsType.Caption,
                    color = NsColor.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun iconBackgroundColor(type: NotificationType): Color = when (type) {
    NotificationType.VaccineAlert -> MaterialTheme.extendedColors.warning
    NotificationType.UpcomingControl -> MaterialTheme.colorScheme.primary
    NotificationType.Advice -> MaterialTheme.colorScheme.tertiary
}

private fun iconRes(type: NotificationType): Int = when (type) {
    NotificationType.VaccineAlert -> R.drawable.ic_tab_vaccines
    NotificationType.UpcomingControl -> R.drawable.ic_calendar
    NotificationType.Advice -> R.drawable.ic_tab_advice
}

@Preview(showBackground = true)
@Composable
private fun NotificationCenterScreenPreview() {
    NsTheme {
        NotificationCenterContent(
            state = NotificationCenterUiState(
                sections = listOf(
                    NotificationSectionUiModel(
                        header = "HOY",
                        items = listOf(
                            NotificationItemUiModel(
                                id = "1",
                                type = NotificationType.VaccineAlert,
                                title = "Vacuna atrasada",
                                body = "La 3ª dosis de Pentavalente de Lucía está atrasada.",
                                relativeTime = "Hace 2 horas",
                            ),
                            NotificationItemUiModel(
                                id = "2",
                                type = NotificationType.UpcomingControl,
                                title = "Próximo control",
                                body = "El control de los 8 meses de Lucía es en 5 días.",
                                relativeTime = "Hace 6 horas",
                            ),
                        ),
                    ),
                    NotificationSectionUiModel(
                        header = "ESTA SEMANA",
                        items = listOf(
                            NotificationItemUiModel(
                                id = "3",
                                type = NotificationType.Advice,
                                title = "Nuevo consejo disponible",
                                body = "\"Introducción de sólidos\" — recomendaciones para esta edad.",
                                relativeTime = "Hace 2 días",
                            ),
                        ),
                    ),
                ),
            ),
            onBackClick = {},
        )
    }
}
