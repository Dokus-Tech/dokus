package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for user avatar endpoints.
 * Base path: /api/v1/users
 *
 * SECURITY:
 * - Reads require authentication
 * - Writes/deletes are self-only
 */
@Serializable
@Resource("/api/v1/users")
class Users {
    /**
     * User-scoped routes.
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Users = Users(), val id: String) {
        /**
         * GET/POST/DELETE /api/v1/users/{id}/avatar
         * GET - Get avatar URLs
         * POST - Upload avatar
         * DELETE - Remove avatar
         */
        @Serializable
        @Resource("avatar")
        class Avatar(val parent: Id)
    }

    /**
     * GET /api/v1/users/{id}/avatar/{size}.webp
     * Stream a user avatar image by size for a specific user.
     */
    @Serializable
    @Resource("{id}/avatar/{size}.webp")
    class AvatarImageById(
        val parent: Users = Users(),
        val id: String,
        val size: String
    )
}
