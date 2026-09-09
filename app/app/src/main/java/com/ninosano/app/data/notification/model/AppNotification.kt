package com.ninosano.app.data.notification.model

import java.time.Instant

enum class NotificationType { VaccineAlert, UpcomingControl, Advice }

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val timestamp: Instant,
)
