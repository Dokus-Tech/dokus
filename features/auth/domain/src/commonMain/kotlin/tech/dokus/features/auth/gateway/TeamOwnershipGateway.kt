package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.UserId

/**
 * Gateway for team ownership transfer operations.
 */
interface TeamOwnershipGateway {
    suspend fun transferOwnership(newOwnerId: UserId): Result<Unit>
}
