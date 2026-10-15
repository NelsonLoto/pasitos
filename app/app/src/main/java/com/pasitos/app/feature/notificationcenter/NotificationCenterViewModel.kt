package com.pasitos.app.feature.notificationcenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitos.app.data.notification.NotificationRepository
import com.pasitos.app.data.notification.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationCenterUiState> = notificationRepository.notifications
        .map { notifications -> NotificationCenterUiState(sections = groupBySection(notifications)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationCenterUiState())

    private fun groupBySection(notifications: List<AppNotification>): List<NotificationSectionUiModel> {
        val now = Instant.now()
        val sorted = notifications.sortedByDescending { it.timestamp }
        val (today, thisWeek) = sorted.partition { Duration.between(it.timestamp, now).toHours() < 24 }

        return listOfNotNull(
            today.takeIf { it.isNotEmpty() }?.let { NotificationSectionUiModel("HOY", it.map { n -> n.toUiModel(now) }) },
            thisWeek.takeIf { it.isNotEmpty() }?.let { NotificationSectionUiModel("ESTA SEMANA", it.map { n -> n.toUiModel(now) }) },
        )
    }

    private fun AppNotification.toUiModel(now: Instant) = NotificationItemUiModel(
        id = id,
        type = type,
        title = title,
        body = body,
        relativeTime = relativeTimeLabel(timestamp, now),
    )

    private fun relativeTimeLabel(timestamp: Instant, now: Instant): String {
        val minutes = Duration.between(timestamp, now).toMinutes()
        return when {
            minutes < 60 -> if (minutes <= 1) "Hace 1 minuto" else "Hace $minutes minutos"
            minutes < 24 * 60 -> {
                val hours = minutes / 60
                if (hours == 1L) "Hace 1 hora" else "Hace $hours horas"
            }
            else -> {
                val days = minutes / (24 * 60)
                if (days == 1L) "Hace 1 día" else "Hace $days días"
            }
        }
    }
}
