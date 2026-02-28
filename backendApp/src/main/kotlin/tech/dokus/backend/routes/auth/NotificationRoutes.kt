package tech.dokus.backend.routes.auth

import tech.dokus.backend.security.requireTenantAccess

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.notifications.NotificationPreferencesService
import tech.dokus.backend.services.notifications.NotificationService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.model.UnreadCountResponse
import tech.dokus.domain.model.UpdateNotificationPreferenceRequest
import tech.dokus.domain.routes.Notifications
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal

internal fun Route.notificationRoutes() {
    val notificationService by inject<NotificationService>()
    val notificationPreferencesService by inject<NotificationPreferencesService>()

    authenticateJwt {
        get<Notifications> { route ->
            val principal = dokusPrincipal
            val tenantId = requireTenantAccess().tenantId

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val response = notificationService.list(
                tenantId = tenantId,
                userId = principal.userId,
                type = route.type,
                category = route.category,
                isRead = route.isRead,
                limit = route.limit,
                offset = route.offset
            ).getOrThrow()

            call.respond(HttpStatusCode.OK, response)
        }

        get<Notifications.UnreadCount> {
            val principal = dokusPrincipal
            val tenantId = requireTenantAccess().tenantId

            val count = notificationService.unreadCount(
                tenantId = tenantId,
                userId = principal.userId
            ).getOrThrow()

            call.respond(HttpStatusCode.OK, UnreadCountResponse(count))
        }

        patch<Notifications.MarkRead> { route ->
            val principal = dokusPrincipal
            val tenantId = requireTenantAccess().tenantId

            notificationService.markRead(
                tenantId = tenantId,
                userId = principal.userId,
                notificationId = NotificationId.parse(route.id)
            ).getOrThrow()

            call.respond(HttpStatusCode.NoContent)
        }

        post<Notifications.MarkAllRead> {
            val principal = dokusPrincipal
            val tenantId = requireTenantAccess().tenantId

            val updated = notificationService.markAllRead(
                tenantId = tenantId,
                userId = principal.userId
            ).getOrThrow()

            call.respond(HttpStatusCode.OK, mapOf("updated" to updated))
        }

        get<Notifications.Preferences> {
            val principal = dokusPrincipal
            val response = notificationPreferencesService.list(principal.userId).getOrThrow()
            call.respond(HttpStatusCode.OK, response)
        }

        patch<Notifications.Preferences.Type> { route ->
            val principal = dokusPrincipal
            val request = call.receive<UpdateNotificationPreferenceRequest>()

            val updated = notificationPreferencesService.update(
                userId = principal.userId,
                type = route.type,
                emailEnabled = request.emailEnabled
            ).getOrThrow()

            call.respond(HttpStatusCode.OK, updated)
        }
    }
}
