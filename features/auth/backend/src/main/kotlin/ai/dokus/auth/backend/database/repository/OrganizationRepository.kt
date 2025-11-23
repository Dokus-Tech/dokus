package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.mappers.TenantMapper.toOrganizationSettings
import ai.dokus.auth.backend.database.mappers.TenantMapper.toTenant
import ai.dokus.auth.backend.database.tables.OrganizationSettingsTable
import ai.dokus.auth.backend.database.tables.OrganizationTable
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class OrganizationRepository {
    private val logger = LoggerFactory.getLogger(OrganizationRepository::class.java)

    suspend fun create(
        name: String,
        email: String,
        plan: OrganizationPlan = OrganizationPlan.Free,
        country: Country,
        language: Language,
        vatNumber: VatNumber?,
    ): OrganizationId = dbQuery {
        val organizationId = OrganizationTable.insertAndGetId {
            it[OrganizationTable.name] = name
            it[OrganizationTable.email] = email
            it[OrganizationTable.plan] = plan
            it[OrganizationTable.country] = country
            it[OrganizationTable.language] = language
            it[OrganizationTable.vatNumber] = vatNumber?.value
            it[status] = TenantStatus.Active
        }.value

        // Create default settings for the tenant
        OrganizationSettingsTable.insert {
            it[OrganizationSettingsTable.organizationId] = organizationId
        }

        logger.info("Created new tenant: $organizationId with email: $email")
        OrganizationId(organizationId.toKotlinUuid())
    }

    suspend fun findById(id: OrganizationId): Organization? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        OrganizationTable
            .selectAll()
            .where { OrganizationTable.id eq javaUuid }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun findByEmail(email: String): Organization? = dbQuery {
        OrganizationTable
            .selectAll()
            .where { OrganizationTable.email eq email }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun getSettings(organizationId: OrganizationId): OrganizationSettings = dbQuery {
        val javaUuid = organizationId.value.toJavaUuid()
        OrganizationSettingsTable
            .selectAll()
            .where { OrganizationSettingsTable.organizationId eq javaUuid }
            .singleOrNull()
            ?.toOrganizationSettings()
            ?: throw IllegalArgumentException("No settings found for tenant: $organizationId")
    }

    suspend fun updateSettings(settings: OrganizationSettings): Unit = dbQuery {
        val javaUuid = settings.organizationId.value.toJavaUuid()
        OrganizationSettingsTable.update({ OrganizationSettingsTable.organizationId eq javaUuid }) {
            it[invoicePrefix] = settings.invoicePrefix
            it[nextInvoiceNumber] = settings.nextInvoiceNumber
            it[defaultPaymentTerms] = settings.defaultPaymentTerms
            it[defaultVatRate] = BigDecimal(settings.defaultVatRate.value)
            it[companyName] = settings.companyName
            it[companyAddress] = settings.companyAddress
            it[companyVatNumber] = settings.companyVatNumber?.value
            it[companyIban] = settings.companyIban?.value
            it[companyBic] = settings.companyBic?.value
            it[companyLogoUrl] = settings.companyLogoUrl
            it[emailInvoiceReminders] = settings.emailInvoiceReminders
            it[emailPaymentConfirmations] = settings.emailPaymentConfirmations
            it[emailWeeklyReports] = settings.emailWeeklyReports
            it[enableBankSync] = settings.enableBankSync
            it[enablePeppol] = settings.enablePeppol
        }
    }

    suspend fun getNextInvoiceNumber(organizationId: OrganizationId): InvoiceNumber = dbQuery {
        val javaUuid = organizationId.value.toJavaUuid()
        val settings = OrganizationSettingsTable
            .selectAll()
            .where { OrganizationSettingsTable.organizationId eq javaUuid }
            .single()

        val prefix = settings[OrganizationSettingsTable.invoicePrefix]
        val number = settings[OrganizationSettingsTable.nextInvoiceNumber]

        // Increment the counter
        OrganizationSettingsTable.update({ OrganizationSettingsTable.organizationId eq javaUuid }) {
            it[nextInvoiceNumber] = nextInvoiceNumber + 1
        }

        InvoiceNumber("$prefix-${number.toString().padStart(4, '0')}")
    }

    suspend fun listActiveTenants(): List<Organization> = dbQuery {
        OrganizationTable
            .selectAll()
            .where { OrganizationTable.status eq TenantStatus.Active }
            .map { it.toTenant() }
    }
}