package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.mappers.TenantMapper.toTenant
import ai.dokus.auth.backend.database.mappers.TenantMapper.toTenantSettings
import ai.dokus.auth.backend.database.tables.TenantSettingsTable
import ai.dokus.auth.backend.database.tables.TenantsTable
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
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
class TenantRepository {
    private val logger = LoggerFactory.getLogger(TenantRepository::class.java)

    suspend fun create(
        name: String,
        email: String,
        plan: TenantPlan = TenantPlan.Free,
        country: String = "BE",
        language: Language = Language.En,
        vatNumber: VatNumber? = null
    ): TenantId = dbQuery {
        val tenantId = TenantsTable.insertAndGetId {
            it[TenantsTable.name] = name
            it[TenantsTable.email] = email
            it[TenantsTable.plan] = plan
            it[TenantsTable.country] = country
            it[TenantsTable.language] = language
            it[TenantsTable.vatNumber] = vatNumber?.value
            it[status] = TenantStatus.Active
        }.value

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
        }

        logger.info("Created new tenant: $tenantId with email: $email")
        TenantId(tenantId.toKotlinUuid())
    }

    suspend fun findById(id: TenantId): Tenant? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        TenantsTable
            .selectAll()
            .where { TenantsTable.id eq javaUuid }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun findByEmail(email: String): Tenant? = dbQuery {
        TenantsTable
            .selectAll()
            .where { TenantsTable.email eq email }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun getSettings(tenantId: TenantId): TenantSettings = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .singleOrNull()
            ?.toTenantSettings()
            ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")
    }

    suspend fun updateSettings(settings: TenantSettings): Unit = dbQuery {
        val javaUuid = settings.tenantId.value.toJavaUuid()
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq javaUuid }) {
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

    suspend fun getNextInvoiceNumber(tenantId: TenantId): InvoiceNumber = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        val settings = TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .single()

        val prefix = settings[TenantSettingsTable.invoicePrefix]
        val number = settings[TenantSettingsTable.nextInvoiceNumber]

        // Increment the counter
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq javaUuid }) {
            it[nextInvoiceNumber] = nextInvoiceNumber + 1
        }

        InvoiceNumber("$prefix-${number.toString().padStart(4, '0')}")
    }

    suspend fun listActiveTenants(): List<Tenant> = dbQuery {
        TenantsTable
            .selectAll()
            .where { TenantsTable.status eq TenantStatus.Active }
            .map { it.toTenant() }
    }
}