package tech.dokus.database.services

import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import tech.dokus.database.tables.auth.TenantSettingsTable
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Configuration for invoice number generation.
 *
 * This data class holds all tenant-specific settings for generating
 * invoice numbers in the required format.
 */
data class InvoiceNumberConfig(
    val prefix: String,
    val yearlyReset: Boolean,
    val padding: Int,
    val includeYear: Boolean,
    val timezone: String
)

/**
 * Service for generating sequential invoice numbers with formatting.
 *
 * This service orchestrates invoice number generation by:
 * 1. Fetching tenant-specific configuration from TenantSettingsTable
 * 2. Getting the next sequence number atomically via InvoiceNumberRepository
 * 3. Formatting the number according to tenant preferences
 *
 * The generated invoice numbers follow the format:
 * - With year: {prefix}-{year}-{padded_sequence} (e.g., INV-2025-0001)
 * - Without year: {prefix}-{padded_sequence} (e.g., INV-0001)
 *
 * CRITICAL: This service ensures Belgian tax law compliance by generating
 * gap-less sequential numbers. All operations are atomic and concurrent-safe.
 */
@OptIn(ExperimentalUuidApi::class)
class InvoiceNumberGenerator(
    private val invoiceNumberRepository: InvoiceNumberRepository
) {
    private val logger = loggerFor()

    /**
     * Generate the next invoice number for a tenant.
     *
     * This method:
     * 1. Fetches the tenant's invoice configuration
     * 2. Calculates the current year in the tenant's timezone
     * 3. Atomically increments and retrieves the next sequence number
     * 4. Formats the invoice number according to configuration
     *
     * @param tenantId The tenant requesting an invoice number
     * @return Result containing the formatted invoice number, or failure on error
     */
    suspend fun generateInvoiceNumber(tenantId: TenantId): Result<String> {
        logger.info("Starting invoice number generation: tenant_id=$tenantId")

        return runCatching {
            // Step 1: Fetch tenant's invoice configuration
            val config = getInvoiceConfig(tenantId).getOrThrow()
            logger.debug("Loaded tenant config: tenant_id=$tenantId, prefix=${config.prefix}, " +
                    "yearly_reset=${config.yearlyReset}, padding=${config.padding}, " +
                    "include_year=${config.includeYear}, timezone=${config.timezone}")

            // Step 2: Calculate current year in tenant's timezone
            val year = getCurrentYear(config.timezone)
            logger.debug("Resolved year: tenant_id=$tenantId, timezone=${config.timezone}, year=$year")

            // Step 3: Determine which year to use for sequence lookup
            // If yearlyReset is disabled, we use year 0 as a "global" sequence
            val sequenceYear = if (config.yearlyReset) year else 0

            // Step 4: Get and increment the sequence number atomically
            val sequenceNumber = invoiceNumberRepository.getAndIncrementSequence(tenantId, sequenceYear).getOrThrow()
            logger.debug("Sequence incremented: tenant_id=$tenantId, sequence_year=$sequenceYear, " +
                    "sequence_number=$sequenceNumber")

            // Step 5: Format the invoice number
            val invoiceNumber = formatInvoiceNumber(
                prefix = config.prefix,
                year = year,
                sequence = sequenceNumber,
                padding = config.padding,
                includeYear = config.includeYear
            )

            // Structured audit log for invoice number generation
            // Contains all fields required for Belgian tax compliance audit trail:
            // timestamp (handled by logging framework), tenant_id, generated_number, sequence_number, year
            logger.info("Invoice number generated: tenant_id=$tenantId, generated_number=$invoiceNumber, " +
                    "sequence_number=$sequenceNumber, year=$year, prefix=${config.prefix}, " +
                    "yearly_reset=${config.yearlyReset}")

            invoiceNumber
        }.onFailure { error ->
            logger.error("Invoice number generation failed: tenant_id=$tenantId, error=${error.message}", error)
        }
    }

    /**
     * Preview the next invoice number without consuming a sequence number.
     *
     * Useful for UI display purposes before the invoice is actually created.
     *
     * @param tenantId The tenant to preview for
     * @return Result containing the predicted next invoice number
     */
    suspend fun previewNextInvoiceNumber(tenantId: TenantId): Result<String> {
        logger.debug("Previewing next invoice number: tenant_id=$tenantId")

        return runCatching {
            val config = getInvoiceConfig(tenantId).getOrThrow()
            val year = getCurrentYear(config.timezone)
            val sequenceYear = if (config.yearlyReset) year else 0

            // Get current sequence (without incrementing)
            val currentSequence = invoiceNumberRepository.getCurrentSequence(tenantId, sequenceYear).getOrThrow()
            val nextSequence = currentSequence + 1

            val previewNumber = formatInvoiceNumber(
                prefix = config.prefix,
                year = year,
                sequence = nextSequence,
                padding = config.padding,
                includeYear = config.includeYear
            )

            logger.debug("Invoice number preview: tenant_id=$tenantId, preview_number=$previewNumber, " +
                    "next_sequence=$nextSequence, year=$year")

            previewNumber
        }.onFailure { error ->
            logger.error("Invoice number preview failed: tenant_id=$tenantId, error=${error.message}", error)
        }
    }

    /**
     * Fetch invoice numbering configuration for a tenant.
     *
     * Reads the configuration directly from TenantSettingsTable.
     *
     * @param tenantId The tenant to fetch configuration for
     * @return Result containing the invoice config, or failure if not found
     */
    suspend fun getInvoiceConfig(tenantId: TenantId): Result<InvoiceNumberConfig> {
        logger.debug("Fetching invoice config: tenant_id=$tenantId")

        return runCatching {
            dbQuery {
                val javaUuid = tenantId.value.toJavaUuid()

                val row = TenantSettingsTable
                    .selectAll()
                    .where { TenantSettingsTable.tenantId eq javaUuid }
                    .singleOrNull()
                    ?: throw IllegalArgumentException("No settings found for tenant: $tenantId")

                InvoiceNumberConfig(
                    prefix = row[TenantSettingsTable.invoicePrefix],
                    yearlyReset = row[TenantSettingsTable.invoiceYearlyReset],
                    padding = row[TenantSettingsTable.invoicePadding],
                    includeYear = row[TenantSettingsTable.invoiceIncludeYear],
                    timezone = row[TenantSettingsTable.invoiceTimezone]
                )
            }
        }.onFailure { error ->
            logger.error("Failed to fetch invoice config: tenant_id=$tenantId, error=${error.message}", error)
        }
    }

    /**
     * Format an invoice number according to the specified parameters.
     *
     * Format patterns:
     * - With year: {prefix}-{year}-{padded_sequence}
     * - Without year: {prefix}-{padded_sequence}
     *
     * @param prefix The invoice prefix (e.g., "INV", "FACT")
     * @param year The year component (used only if includeYear is true)
     * @param sequence The sequence number
     * @param padding The minimum number of digits for the sequence (zero-padded)
     * @param includeYear Whether to include the year in the formatted number
     * @return The formatted invoice number string
     */
    fun formatInvoiceNumber(
        prefix: String,
        year: Int,
        sequence: Int,
        padding: Int,
        includeYear: Boolean
    ): String {
        val paddedSequence = sequence.toString().padStart(padding, '0')

        return if (includeYear) {
            "$prefix-$year-$paddedSequence"
        } else {
            "$prefix-$paddedSequence"
        }
    }

    /**
     * Get the current year in the specified timezone.
     *
     * This ensures that invoice numbers roll over at the correct time
     * based on the tenant's local timezone, not the server timezone.
     *
     * Belgian tax law requires invoices to be numbered based on the
     * Belgian timezone (Europe/Brussels).
     *
     * @param timezone The IANA timezone identifier (e.g., "Europe/Brussels")
     * @return The current year in the specified timezone
     */
    fun getCurrentYear(timezone: String): Int {
        return try {
            val zoneId = ZoneId.of(timezone)
            ZonedDateTime.now(zoneId).year
        } catch (e: Exception) {
            // Fallback to Europe/Brussels if invalid timezone
            logger.warn("Invalid timezone '$timezone', falling back to Europe/Brussels", e)
            ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        }
    }
}
