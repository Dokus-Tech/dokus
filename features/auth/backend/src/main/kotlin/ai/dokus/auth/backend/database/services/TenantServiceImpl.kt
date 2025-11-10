@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.database.services

import ai.dokus.auth.backend.database.mappers.TenantMapper.toTenant
import ai.dokus.auth.backend.database.mappers.TenantMapper.toTenantSettings
import ai.dokus.auth.backend.database.tables.TenantSettingsTable
import ai.dokus.auth.backend.database.tables.TenantsTable
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.ktor.services.TenantService
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlin.time.ExperimentalTime

class TenantServiceImpl : TenantService {
    private val logger = LoggerFactory.getLogger(TenantServiceImpl::class.java)

    override suspend fun createTenant(
        name: String,
        email: String,
        plan: TenantPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Tenant {
        // For Free plan, start with Trial status and 30-day trial period
        // Other plans are Active immediately
        val isTrial = plan == TenantPlan.Free
        val tenantStatus = if (isTrial) TenantStatus.Trial else TenantStatus.Active
        val trialEndsAt = if (isTrial) {
            val now = kotlinx.datetime.Clock.System.now()
            val trialPeriod = DateTimePeriod(days = 30)
            now.plus(trialPeriod, TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
        } else null

        return dbQuery {
            val tenantId = TenantsTable.insertAndGetId {
            it[TenantsTable.name] = name
            it[TenantsTable.email] = email
            it[TenantsTable.plan] = plan
            it[TenantsTable.country] = country
            it[TenantsTable.language] = language
            it[TenantsTable.vatNumber] = vatNumber?.value
            it[status] = tenantStatus
            it[TenantsTable.trialEndsAt] = trialEndsAt
        }.value

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
        }

            logger.info("Created new tenant: $tenantId with email: $email, plan: $plan, status: $tenantStatus${if (trialEndsAt != null) ", trial ends at: $trialEndsAt" else ""}")

            // Return the created tenant
            TenantsTable
                .selectAll()
                .where { TenantsTable.id eq tenantId }
                .single()
                .toTenant()
        }
    }

    override suspend fun findById(id: TenantId): Tenant? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        TenantsTable
            .selectAll()
            .where { TenantsTable.id eq javaUuid }
            .singleOrNull()
            ?.toTenant()
    }

    override suspend fun findByEmail(email: String): Tenant? = dbQuery {
        TenantsTable
            .selectAll()
            .where { TenantsTable.email eq email }
            .singleOrNull()
            ?.toTenant()
    }

    override suspend fun updateSettings(settings: TenantSettings) = dbQuery {
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
        logger.info("Updated settings for tenant: ${settings.tenantId}")
    }

    override suspend fun getSettings(tenantId: TenantId): TenantSettings = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .singleOrNull()
            ?.toTenantSettings()
            ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")
    }

    override suspend fun getNextInvoiceNumber(tenantId: TenantId): InvoiceNumber = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        val settings = TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .single()

        val prefix = settings[TenantSettingsTable.invoicePrefix]
        val number = settings[TenantSettingsTable.nextInvoiceNumber]

        // Increment the counter
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq javaUuid }) {
            with(SqlExpressionBuilder) {
                it[nextInvoiceNumber] = nextInvoiceNumber + 1
            }
        }

        logger.debug("Generated invoice number for tenant $tenantId: $prefix-${number.toString().padStart(4, '0')}")
        InvoiceNumber("$prefix-${number.toString().padStart(4, '0')}")
    }

    override suspend fun listActiveTenants(): List<Tenant> = dbQuery {
        TenantsTable
            .selectAll()
            .where { TenantsTable.status eq TenantStatus.Active }
            .map { it.toTenant() }
    }
}
