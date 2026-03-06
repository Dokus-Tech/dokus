package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem

/**
 * Gateway for company-owner management of bookkeeper firm access.
 */
interface TeamBookkeeperAccessGateway {
    suspend fun searchBookkeeperFirms(
        query: String,
        limit: Int = 20,
    ): Result<List<BookkeeperFirmSearchItem>>

    suspend fun listBookkeeperAccess(): Result<List<TenantBookkeeperAccessItem>>

    suspend fun grantBookkeeperAccess(firmId: FirmId): Result<GrantBookkeeperAccessResponse>

    suspend fun revokeBookkeeperAccess(firmId: FirmId): Result<Unit>
}
