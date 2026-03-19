package tech.dokus.backend.services.admin

import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest

/**
 * Service layer for tenant management operations.
 * Wraps repository calls used by TenantRoutes and AvatarRoutes.
 */
class TenantManagementService(
    private val tenantRepository: TenantRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository,
) {
    // ── Tenant queries ──────────────────────────────────────────────────

    suspend fun findTenantById(tenantId: TenantId): Tenant? =
        tenantRepository.findById(tenantId)

    suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        subscription: SubscriptionTier,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): TenantId = tenantRepository.create(
        type = type,
        legalName = legalName,
        displayName = displayName,
        subscription = subscription,
        language = language,
        vatNumber = vatNumber,
        address = address,
    )

    suspend fun getSettings(tenantId: TenantId): TenantSettings =
        tenantRepository.getSettings(tenantId)

    suspend fun updateSettings(settings: TenantSettings) =
        tenantRepository.updateSettings(settings)

    // ── Address ─────────────────────────────────────────────────────────

    suspend fun getCompanyAddress(tenantId: TenantId): Address? =
        addressRepository.getCompanyAddress(tenantId)

    suspend fun upsertCompanyAddress(tenantId: TenantId, request: UpsertTenantAddressRequest): Address =
        addressRepository.upsertCompanyAddress(tenantId, request)

    // ── User / membership ───────────────────────────────────────────────

    suspend fun getUserTenants(userId: UserId): List<TenantMembership> =
        userRepository.getUserTenants(userId)

    suspend fun getMembership(userId: UserId, tenantId: TenantId): TenantMembership? =
        userRepository.getMembership(userId, tenantId)

    suspend fun addUserToTenant(userId: UserId, tenantId: TenantId, role: UserRole) =
        userRepository.addToTenant(userId, tenantId, role)

    // ── Avatar storage keys ─────────────────────────────────────────────

    suspend fun getTenantAvatarStorageKey(tenantId: TenantId): String? =
        tenantRepository.getAvatarStorageKey(tenantId)

    suspend fun updateTenantAvatarStorageKey(tenantId: TenantId, storageKey: String?) =
        tenantRepository.updateAvatarStorageKey(tenantId, storageKey)

    suspend fun getUserAvatarStorageKey(userId: UserId): String? =
        userRepository.getAvatarStorageKey(userId)

    suspend fun updateUserAvatarStorageKey(userId: UserId, storageKey: String?) =
        userRepository.updateAvatarStorageKey(userId, storageKey)
}
