package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import ai.dokus.foundation.domain.rpc.OrganizationRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedOrganizationId
import ai.dokus.foundation.ktor.security.requireAuthenticatedUserId
import org.slf4j.LoggerFactory

class OrganizationRemoteServiceImpl(
    private val organizationService: OrganizationRepository,
    private val userRepository: UserRepository,
    private val authInfoProvider: AuthInfoProvider,
) : OrganizationRemoteService {

    private val logger = LoggerFactory.getLogger(OrganizationRemoteServiceImpl::class.java)

    override suspend fun createOrganization(
        name: String,
        email: String,
        plan: OrganizationPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Organization {
        return authInfoProvider.withAuthInfo {
            // Get the authenticated user who is creating the organization
            val userId = requireAuthenticatedUserId()

            // Create the organization
            val organizationId = organizationService.create(name, email, plan, country, language, vatNumber)

            // Add the creating user as Owner of the new organization
            userRepository.addToOrganization(userId, organizationId, UserRole.Owner)
            logger.info("User $userId created organization $organizationId and became Owner")

            organizationService.findById(id = organizationId)
                ?: throw IllegalArgumentException("Organization not found: $organizationId")
        }
    }

    override suspend fun getOrganization(id: OrganizationId): Organization {
        return organizationService.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getOrganizationSettings(): OrganizationSettings {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            return@withAuthInfo organizationService.getSettings(organizationId)
        }
    }

    override suspend fun updateOrganizationSettings(settings: OrganizationSettings) {
        organizationService.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            return@withAuthInfo organizationService.getNextInvoiceNumber(organizationId)
        }
    }
}