@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedTenantId
import ai.dokus.foundation.ktor.security.requireAuthenticatedUserId
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

class TenantRemoteServiceImpl(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val authInfoProvider: AuthInfoProvider,
) : TenantRemoteService {

    private val logger = LoggerFactory.getLogger(TenantRemoteServiceImpl::class.java)

    override suspend fun listMyTenants(): List<Tenant> {
        return authInfoProvider.withAuthInfo {
            val userId = requireAuthenticatedUserId()
            logger.debug("Listing tenants for user: {}", userId.value)
            val memberships = userRepository.getUserTenants(userId).filter { it.isActive }
            memberships.mapNotNull { membership ->
                tenantRepository.findById(membership.tenantId)
            }
        }
    }

    override suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan,
        country: Country,
        language: Language,
        vatNumber: VatNumber
    ): Tenant {
        return authInfoProvider.withAuthInfo {
            // Get the authenticated user creating the tenant
            val userId = requireAuthenticatedUserId()

            // Create the tenant
            val tenantId = tenantRepository.create(
                type = type,
                legalName = legalName,
                displayName = displayName,
                plan = plan,
                country = country,
                language = language,
                vatNumber = vatNumber
            )

            // Add the creating user as Owner of the new tenant
            userRepository.addToTenant(userId, tenantId, UserRole.Owner)
            logger.info("User $userId created tenant $tenantId and became Owner")

            tenantRepository.findById(id = tenantId)
                ?: throw IllegalArgumentException("Tenant not found: $tenantId")
        }
    }

    override suspend fun getTenant(id: TenantId): Tenant {
        return tenantRepository.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getTenantSettings(): TenantSettings {
        return authInfoProvider.withAuthInfo {
            val tenantId = requireAuthenticatedTenantId()
            return@withAuthInfo tenantRepository.getSettings(tenantId)
        }
    }

    override suspend fun updateTenantSettings(settings: TenantSettings) {
        tenantRepository.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber {
        return authInfoProvider.withAuthInfo {
            val tenantId = requireAuthenticatedTenantId()
            return@withAuthInfo tenantRepository.getNextInvoiceNumber(tenantId)
        }
    }
}
