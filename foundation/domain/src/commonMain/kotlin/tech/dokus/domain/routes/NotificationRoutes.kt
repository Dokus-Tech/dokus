package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType

@Serializable
@Resource("/api/v1/notifications")
class Notifications(
    val type: NotificationType? = null,
    val category: NotificationCategory? = null,
    val isRead: Boolean? = null,
    val limit: Int = 20,
    val offset: Int = 0
) {
    @Serializable
    @Resource("unread-count")
    class UnreadCount(val parent: Notifications = Notifications())

    @Serializable
    @Resource("{id}/read")
    class MarkRead(val parent: Notifications = Notifications(), val id: String)

    @Serializable
    @Resource("mark-all-read")
    class MarkAllRead(val parent: Notifications = Notifications())

    @Serializable
    @Resource("preferences")
    class Preferences(val parent: Notifications = Notifications()) {
        @Serializable
        @Resource("{type}")
        class Type(val parent: Preferences, val type: NotificationType)
    }
}
