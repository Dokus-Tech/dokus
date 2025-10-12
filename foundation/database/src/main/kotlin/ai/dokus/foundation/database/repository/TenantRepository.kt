package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.enums.Language
import ai.dokus.foundation.database.enums.TenantPlan
import ai.dokus.foundation.database.enums.TenantStatus
import ai.dokus.foundation.database.mappers.TenantMapper.toTenant
import ai.dokus.foundation.database.mappers.TenantMapper.toTenantSettings
import ai.dokus.foundation.database.tables.TenantsTable
import ai.dokus.foundation.database.tables.TenantSettingsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID

class TenantRepository {
    private val logger = LoggerFactory.getLogger(TenantRepository::class.java)

    suspend fun create(
        name: String,
        email: String,
        plan: TenantPlan = TenantPlan.FREE,
        country: String = "BE",
        language: Language = Language.EN,
        vatNumber: String? = null
    ): String = dbQuery {
        val tenantId = TenantsTable.insertAndGetId {
            it[TenantsTable.name] = name
            it[TenantsTable.email] = email
            it[TenantsTable.plan] = plan
            it[TenantsTable.country] = country
            it[TenantsTable.language] = language
            it[TenantsTable.vatNumber] = vatNumber
            it[status] = TenantStatus.ACTIVE
        }.value

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
        }

        logger.info("Created new tenant: $tenantId with email: $email")
        tenantId.toString()
    }

    suspend fun findById(id: String): Tenant? = dbQuery {
        val uuid = UUID.fromString(id)
        TenantsTable
            .selectAll()
            .where { TenantsTable.id eq uuid }
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

    suspend fun getSettings(tenantId: String): TenantSettings = dbQuery {
        val uuid = UUID.fromString(tenantId)
        TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq uuid }
            .singleOrNull()
            ?.toTenantSettings()
            ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")
    }

    suspend fun updateSettings(settings: TenantSettings): Unit = dbQuery {
        val uuid = UUID.fromString(settings.tenantId)
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq uuid }) {
            it[invoicePrefix] = settings.invoicePrefix
            it[nextInvoiceNumber] = settings.nextInvoiceNumber
            it[defaultPaymentTerms] = settings.defaultPaymentTerms
            it[defaultVatRate] = BigDecimal(settings.defaultVatRate)
            it[companyName] = settings.companyName
            it[companyVatNumber] = settings.companyVatNumber
            it[companyIban] = settings.companyIban
            it[companyBic] = settings.companyBic
            it[companyLogoUrl] = settings.companyLogoUrl
            it[emailInvoiceReminders] = settings.emailInvoiceReminders
            it[emailPaymentConfirmations] = settings.emailPaymentConfirmations
            it[emailWeeklyReports] = settings.emailWeeklyReports
            it[enableBankSync] = settings.enableBankSync
            it[enablePeppol] = settings.enablePeppol
        }
    }

    suspend fun getNextInvoiceNumber(tenantId: String): String = dbQuery {
        val uuid = UUID.fromString(tenantId)
        val settings = TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq uuid }
            .single()

        val prefix = settings[TenantSettingsTable.invoicePrefix]
        val number = settings[TenantSettingsTable.nextInvoiceNumber]

        // Increment the counter
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq uuid }) {
            with(SqlExpressionBuilder) {
                it[nextInvoiceNumber] = nextInvoiceNumber + 1
            }
        }

        "$prefix-${number.toString().padStart(4, '0')}"
    }

    suspend fun listActiveTenants(): List<Tenant> = dbQuery {
        TenantsTable
            .selectAll()
            .where { TenantsTable.status eq TenantStatus.ACTIVE }
            .map { it.toTenant() }
    }
}