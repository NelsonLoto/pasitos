package com.ninosano.app.data.notification

import com.ninosano.app.data.notification.model.AppNotification
import com.ninosano.app.data.notification.model.NotificationType
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeNotificationRepository @Inject constructor() : NotificationRepository {

    override val notifications: StateFlow<List<AppNotification>> = MutableStateFlow(
        listOf(
            AppNotification(
                id = "notif-1",
                type = NotificationType.VaccineAlert,
                title = "Vacuna atrasada",
                body = "La 3ª dosis de Pentavalente de Lucía está atrasada.",
                timestamp = Instant.now().minus(2, ChronoUnit.HOURS),
            ),
            AppNotification(
                id = "notif-2",
                type = NotificationType.UpcomingControl,
                title = "Próximo control",
                body = "El control de los 8 meses de Lucía es en 5 días.",
                timestamp = Instant.now().minus(6, ChronoUnit.HOURS),
            ),
            AppNotification(
                id = "notif-3",
                type = NotificationType.Advice,
                title = "Nuevo consejo disponible",
                body = "\"Introducción de sólidos\" — recomendaciones para esta edad.",
                timestamp = Instant.now().minus(2, ChronoUnit.DAYS),
            ),
            AppNotification(
                id = "notif-4",
                type = NotificationType.VaccineAlert,
                title = "Vacuna próxima a vencer",
                body = "Antigripal pendiente para Lucía (6 meses).",
                timestamp = Instant.now().minus(3, ChronoUnit.DAYS),
            ),
        ),
    ).asStateFlow()
}
