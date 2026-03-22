package tech.dokus.database.repository.auth

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.database.tables.auth.TenantSettingsTable
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class TenantRepository {
    private val logger = loggerFor()

    /**
     * Create a new tenant with required address.
     * All operations are atomic within the same transaction.
     */
    suspend fun create(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        subscription: SubscriptionTier,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): TenantId = dbQuery {
        // Create tenant
        val tenantId = TenantTable.insertAndGetId {
            it[TenantTable.type] = type
            it[TenantTable.legalName] = legalName.value
            it[TenantTable.displayName] = displayName.value
            it[TenantTable.plan] = subscription
            it[TenantTable.language] = language
            it[TenantTable.vatNumber] = vatNumber.value
            it[status] = TenantStatus.Active
        }.value

        // Create address (required for all tenants)
        // Address ID = tenant ID for 1:1 lookup
        AddressTable.insert {
            it[AddressTable.id] = tenantId
            it[AddressTable.tenantId] = tenantId
            it[streetLine1] = address.streetLine1
            it[streetLine2] = address.streetLine2
            it[city] = address.city
            it[postalCode] = address.postalCode
            it[country] = address.country.dbValue  // Convert enum to ISO-2 string
        }

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
            it[TenantSettingsTable.cashflowTrackingStartDate] =
                computeCashflowStartDate(Clock.System.todayIn(TimeZone.UTC))
        }

        logger.info("Created new tenant: $tenantId with address")
        TenantId(tenantId.toKotlinUuid())
    }

    suspend fun findById(id: TenantId): Tenant? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        val tenantRow = TenantTable
            .selectAll()
            .where { TenantTable.id eq javaUuid }
            .singleOrNull()
            ?: return@dbQuery null

        Tenant.from(tenantRow)
    }

    suspend fun findByIds(ids: List<TenantId>): List<Tenant> = dbQuery {
        if (ids.isEmpty()) return@dbQuery emptyList()
        val javaUuids = ids.map { it.value.toJavaUuid() }
        TenantTable
            .selectAll()
            .where { TenantTable.id inList javaUuids }
            .map { Tenant.from(it) }
    }

    suspend fun getSettings(tenantId: TenantId): TenantSettings = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .singleOrNull()
            ?.let { TenantSettings.from(it) }
            ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")
    }

    suspend fun updateSettings(settings: TenantSettings): Unit = dbQuery {
        val javaUuid = settings.tenantId.value.toJavaUuid()
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq javaUuid }) {
            it[invoicePrefix] = settings.invoicePrefix
            it[nextInvoiceNumber] = settings.nextInvoiceNumber
            it[defaultPaymentTerms] = settings.defaultPaymentTerms
            it[defaultVatRate] = settings.defaultVatRate.toDbDecimal()
            it[invoiceYearlyReset] = settings.invoiceYearlyReset
            it[invoicePadding] = settings.invoicePadding
            it[invoiceIncludeYear] = settings.invoiceIncludeYear
            it[invoiceTimezone] = settings.invoiceTimezone
            it[companyName] = settings.companyName
            it[companyIban] = settings.companyIban?.value
            it[companyBic] = settings.companyBic?.value
            it[companyLogoUrl] = settings.companyLogoUrl
            it[emailInvoiceReminders] = settings.emailInvoiceReminders
            it[emailPaymentConfirmations] = settings.emailPaymentConfirmations
            it[emailWeeklyReports] = settings.emailWeeklyReports
            it[enableBankSync] = settings.enableBankSync
            it[enablePeppol] = settings.enablePeppol
            it[paymentTermsText] = settings.paymentTermsText
        }
    }

    suspend fun listActiveTenants(): List<Tenant> = dbQuery {
        TenantTable
            .selectAll()
            .where { TenantTable.status eq TenantStatus.Active }
            .map { Tenant.from(it) }
    }

    /**
     * Update the company avatar storage key.
     * @param tenantId The tenant to update
     * @param avatarStorageKey The MinIO storage key prefix for the avatar, or null to remove
     */
    suspend fun updateAvatarStorageKey(tenantId: TenantId, avatarStorageKey: String?): Unit = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantSettingsTable.update({ TenantSettingsTable.tenantId eq javaUuid }) {
            it[companyLogoUrl] = avatarStorageKey
        }
        logger.info("Updated avatar for tenant: $tenantId, key=$avatarStorageKey")
    }

    /**
     * Get the company avatar storage key for a tenant.
     * @param tenantId The tenant to query
     * @return The storage key prefix, or null if no avatar is set
     */
    suspend fun getAvatarStorageKey(tenantId: TenantId): String? = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .singleOrNull()
            ?.get(TenantSettingsTable.companyLogoUrl)
    }

    suspend fun updateWebsiteUrl(tenantId: TenantId, websiteUrl: String?): Unit = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        TenantTable.update({ TenantTable.id eq javaUuid }) {
            it[TenantTable.websiteUrl] = websiteUrl
        }
    }

    /**
     * Returns the effective cashflow tracking start date for a tenant.
     * If stored, returns the stored value. Otherwise computes from tenant creation date.
     *
     * All services must call this single method — no duplicate fallback logic.
     */
    suspend fun getCashflowTrackingStartDate(tenantId: TenantId): LocalDate = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        val stored = TenantSettingsTable
            .selectAll()
            .where { TenantSettingsTable.tenantId eq javaUuid }
            .singleOrNull()
            ?.get(TenantSettingsTable.cashflowTrackingStartDate)

        stored ?: run {
            val tenantCreatedAt = TenantTable
                .selectAll()
                .where { TenantTable.id eq javaUuid }
                .single()[TenantTable.createdAt]
            computeCashflowStartDate(tenantCreatedAt.date)
        }
    }

}

/**
 * Defines the start of the tenant's active cashflow tracking window.
 * We include a 3-month backfill so matching and cashflow provide value immediately,
 * without requiring full historical reconciliation.
 *
 * This is not historical truth. This is a seeded tracking window to provide immediate value.
 *
 * @param creationDate The date the workspace was created
 * @return First day of the month, 3 months before creation
 */
fun computeCashflowStartDate(creationDate: LocalDate): LocalDate {
    val threeMonthsAgo = creationDate.minus(DatePeriod(months = 3))
    return LocalDate(threeMonthsAgo.year, threeMonthsAgo.month, 1)
}
