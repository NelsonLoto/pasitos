package com.ninosano.app.feature.notificationcenter

import com.ninosano.app.data.notification.model.NotificationType

data class NotificationItemUiModel(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val relativeTime: String,
)

data class NotificationSectionUiModel(
    val header: String,
    val items: List<NotificationItemUiModel>,
)

data class NotificationCenterUiState(
    val sections: List<NotificationSectionUiModel> = emptyList(),
)
