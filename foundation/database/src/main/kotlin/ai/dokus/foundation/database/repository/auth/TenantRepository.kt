package ai.dokus.foundation.database.repository.auth

import ai.dokus.foundation.database.mappers.auth.TenantMappers.toTenant
import ai.dokus.foundation.database.mappers.auth.TenantMappers.toTenantSettings
import ai.dokus.foundation.database.tables.auth.AddressTable
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.TenantPlan
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.foundation.ktor.database.dbQuery
import tech.dokus.foundation.ktor.utils.loggerFor
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Configuration for invoice number generation.
 *
 * This data class holds all tenant-specific settings for generating
 * invoice numbers in the required format for Belgian tax compliance.
 */
data class TenantInvoiceConfig(
    val prefix: String,
    val yearlyReset: Boolean,
    val padding: Int,
    val includeYear: Boolean,
    val timezone: String
)

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
        plan: TenantPlan = TenantPlan.Free,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): TenantId = dbQuery {
        // Create tenant
        val tenantId = TenantTable.insertAndGetId {
            it[TenantTable.type] = type
            it[TenantTable.legalName] = legalName.value
            it[TenantTable.displayName] = displayName.value
            it[TenantTable.plan] = plan
            it[TenantTable.language] = language
            it[TenantTable.vatNumber] = vatNumber.value
            it[status] = TenantStatus.Active
        }.value

        // Create address (required for all tenants)
        AddressTable.insert {
            it[AddressTable.tenantId] = tenantId
            it[streetLine1] = address.streetLine1
            it[streetLine2] = address.streetLine2
            it[city] = address.city
            it[postalCode] = address.postalCode
            it[country] = address.country
        }

        // Create default settings for the tenant
        TenantSettingsTable.insert {
            it[TenantSettingsTable.tenantId] = tenantId
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

        tenantRow.toTenant()
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
        TenantTable
            .selectAll()
            .where { TenantTable.status eq TenantStatus.Active }
            .map { it.toTenant() }
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

    /**
     * Fetch invoice numbering configuration for a tenant.
     *
     * This method retrieves all the settings needed for generating
     * invoice numbers according to Belgian tax compliance requirements.
     *
     * @param tenantId The tenant to fetch configuration for
     * @return Result containing the invoice config, or failure if not found
     */
    suspend fun getInvoiceConfig(tenantId: TenantId): Result<TenantInvoiceConfig> = runCatching {
        dbQuery {
            val javaUuid = tenantId.value.toJavaUuid()

            val row = TenantSettingsTable
                .selectAll()
                .where { TenantSettingsTable.tenantId eq javaUuid }
                .singleOrNull()
                ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")

            TenantInvoiceConfig(
                prefix = row[TenantSettingsTable.invoicePrefix],
                yearlyReset = row[TenantSettingsTable.invoiceYearlyReset],
                padding = row[TenantSettingsTable.invoicePadding],
                includeYear = row[TenantSettingsTable.invoiceIncludeYear],
                timezone = row[TenantSettingsTable.invoiceTimezone]
            )
        }
    }
}
