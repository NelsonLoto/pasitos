package com.ninosano.app.data.notification

import com.ninosano.app.data.notification.model.AppNotification
import kotlinx.coroutines.flow.StateFlow

/**
 * Centro de notificaciones (RF12, RF13). Mismo patrón mock/local que el resto de los
 * repositorios — el día que exista el backend (o Firebase Cloud Messaging real), esto
 * pasa a venir de ahí en vez de ser un feed fijo.
 */
interface NotificationRepository {
    val notifications: StateFlow<List<AppNotification>>
}
