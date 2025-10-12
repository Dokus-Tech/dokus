package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.tables.TenantsTable
import ai.dokus.foundation.database.tables.TenantSettingsTable
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID

data class Tenant(
    val id: UUID,
    val name: String,
    val email: String,
    val plan: String,
    val status: String,
    val country: String,
    val language: String,
    val vatNumber: String? = null
)

data class TenantSettings(
    val tenantId: UUID,
    val invoicePrefix: String = "INV",
    val nextInvoiceNumber: Int = 1,
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: BigDecimal = BigDecimal("21.00"),
    val companyName: String? = null,
    val companyVatNumber: String? = null,
    val companyIban: String? = null,
    val enableBankSync: Boolean = false,
    val enablePeppol: Boolean = false
)

class TenantRepository {
    private val logger = LoggerFactory.getLogger(TenantRepository::class.java)

    suspend fun create(
        name: String,
        email: String,
        plan: String = "free",
        country: String = "BE",
        language: String = "en",
        vatNumber: String? = null
    ): UUID = dbQuery {
        val tenantId = TenantsTable.insertAndGetId {
            it[TenantsTable.name] = name
            it[TenantsTable.email] = email
            it[TenantsTable.plan] = plan
            it[TenantsTable.country] = country
            it[TenantsTable.language] = language
            it[TenantsTable.vatNumber] = vatNumber
            it[status] = "active"
        }.value

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
        }

        logger.info("Created new tenant: $tenantId with email: $email")
        tenantId
    }

    suspend fun findById(id: UUID): Tenant? = dbQuery {
        TenantsTable
            .select { TenantsTable.id eq id }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun findByEmail(email: String): Tenant? = dbQuery {
        TenantsTable
            .select { TenantsTable.email eq email }
            .singleOrNull()
            ?.toTenant()
    }

    suspend fun getSettings(tenantId: UUID): TenantSettings = dbQuery {
        TenantSettingsTable
            .select { TenantSettingsTable.tenantId eq tenantId }
            .singleOrNull()
            ?.toTenantSettings()
            ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")
    }

    suspend fun updateSettings(settings: TenantSettings): Unit = dbQuery {
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq settings.tenantId }) {
            it[invoicePrefix] = settings.invoicePrefix
            it[nextInvoiceNumber] = settings.nextInvoiceNumber
            it[defaultPaymentTerms] = settings.defaultPaymentTerms
            it[defaultVatRate] = settings.defaultVatRate
            it[companyName] = settings.companyName
            it[companyVatNumber] = settings.companyVatNumber
            it[companyIban] = settings.companyIban
            it[enableBankSync] = settings.enableBankSync
            it[enablePeppol] = settings.enablePeppol
        }
    }

    suspend fun getNextInvoiceNumber(tenantId: UUID): String = dbQuery {
        val settings = TenantSettingsTable
            .select { TenantSettingsTable.tenantId eq tenantId }
            .single()

        val prefix = settings[TenantSettingsTable.invoicePrefix]
        val number = settings[TenantSettingsTable.nextInvoiceNumber]

        // Increment the counter
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq tenantId }) {
            with(SqlExpressionBuilder) {
                it[nextInvoiceNumber] = nextInvoiceNumber + 1
            }
        }

        "$prefix-${number.toString().padStart(4, '0')}"
    }

    suspend fun listActiveTenants(): List<Tenant> = dbQuery {
        TenantsTable
            .select { TenantsTable.status eq "active" }
            .map { it.toTenant() }
    }

    private fun ResultRow.toTenant() = Tenant(
        id = this[TenantsTable.id].value,
        name = this[TenantsTable.name],
        email = this[TenantsTable.email],
        plan = this[TenantsTable.plan],
        status = this[TenantsTable.status],
        country = this[TenantsTable.country],
        language = this[TenantsTable.language],
        vatNumber = this[TenantsTable.vatNumber]
    )

    private fun ResultRow.toTenantSettings() = TenantSettings(
        tenantId = this[TenantSettingsTable.tenantId].value,
        invoicePrefix = this[TenantSettingsTable.invoicePrefix],
        nextInvoiceNumber = this[TenantSettingsTable.nextInvoiceNumber],
        defaultPaymentTerms = this[TenantSettingsTable.defaultPaymentTerms],
        defaultVatRate = this[TenantSettingsTable.defaultVatRate],
        companyName = this[TenantSettingsTable.companyName],
        companyVatNumber = this[TenantSettingsTable.companyVatNumber],
        companyIban = this[TenantSettingsTable.companyIban],
        enableBankSync = this[TenantSettingsTable.enableBankSync],
        enablePeppol = this[TenantSettingsTable.enablePeppol]
    )
}