@file:Suppress("UseRequire") // Custom exception messaging

package tech.dokus.database.repository.contacts
import kotlin.uuid.Uuid

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.domain.model.contact.ContactStats
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.foundation.backend.database.dbQuery
import java.math.BigDecimal

/**
 * Repository for managing contacts (customers AND vendors)
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return contacts from different tenants
 * 3. All operations must be tenant-isolated
 */
class ContactRepository {

    /**
     * Create a new contact.
     * Note: Addresses are managed separately via ContactAddressRepository.
     * CRITICAL: MUST include tenant_id for multi-tenancy security.
     */
    suspend fun createContact(
        tenantId: TenantId,
        request: CreateContactRequest
    ): Result<ContactDto> = runCatching {
        dbQuery {
            val contactId = ContactsTable.insertAndGetId {
                it[ContactsTable.tenantId] = Uuid.parse(tenantId.toString())
                it[name] = request.name.value
                it[email] = request.email?.value
                it[iban] = request.iban?.value
                it[phone] = request.phone?.value
                it[vatNumber] = request.vatNumber?.value
                // contactType removed - roles are derived from cashflow items
                it[businessType] = request.businessType
                // Addresses are now in ContactAddressesTable join table
                it[contactPerson] = request.contactPerson
                it[companyNumber] = request.companyNumber
                it[defaultPaymentTerms] = request.defaultPaymentTerms
                it[defaultVatRate] = request.defaultVatRate?.let { rate -> BigDecimal(rate) }
                it[tags] = request.tags
                it[contactSource] = request.source
            }

            // Fetch and return the created contact (addresses populated by caller)
            ContactsTable.selectAll().where {
                (ContactsTable.id eq contactId.value) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.single().let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * Get a single contact by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<ContactDto?> = runCatching {
        dbQuery {
            ContactsTable.selectAll().where {
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * List contacts for a tenant with optional filters
     * CRITICAL: MUST filter by tenant_id
     * Note: contactType removed - use derived roles for filtering by role
     */
    suspend fun listContacts(
        tenantId: TenantId,
        isActive: Boolean? = null,
        searchQuery: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        dbQuery {
            var query = ContactsTable.selectAll().where {
                ContactsTable.tenantId eq Uuid.parse(tenantId.toString())
            }

            // Apply filters (contactType removed - roles are derived)
            if (isActive != null) {
                query = query.andWhere { ContactsTable.isActive eq isActive }
            }
            // NOTE: peppolEnabled filter removed - PEPPOL status is now in PeppolDirectoryCacheTable
            if (!searchQuery.isNullOrBlank()) {
                query = query.andWhere {
                    (ContactsTable.name like "%$searchQuery%") or
                        (ContactsTable.email like "%$searchQuery%") or
                        (ContactsTable.vatNumber like "%$searchQuery%")
                }
            }

            val total = query.count()

            val items = query.orderBy(ContactsTable.name to SortOrder.ASC)
                .limit(limit + offset)
                .map { row -> mapRowToContactDto(row) }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * List customers - contacts with outgoing invoices.
     * TODO: Implement proper derived role filtering with JOIN to InvoicesTable
     * For now, returns all active contacts (caller should filter by derived roles)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listCustomers(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        // TODO: Proper implementation requires JOIN with InvoicesTable to find contacts with invoices
        // For now, delegate to listContacts
        listContacts(tenantId, isActive, null, limit, offset).getOrThrow()
    }

    /**
     * List vendors - contacts with incoming invoices/expenses.
     * TODO: Implement proper derived role filtering with JOIN to InvoicesTable/ExpensesTable
     * For now, returns all active contacts (caller should filter by derived roles)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listVendors(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        // TODO: Proper implementation requires JOIN with InvoicesTable/ExpensesTable
        // For now, delegate to listContacts
        listContacts(tenantId, isActive, null, limit, offset).getOrThrow()
    }

    /**
     * Update a contact.
     * Note: Addresses are managed separately via ContactAddressRepository.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateContact(
        contactId: ContactId,
        tenantId: TenantId,
        request: UpdateContactRequest
    ): Result<ContactDto> = runCatching {
        dbQuery {
            // Verify contact exists and belongs to tenant
            val exists = ContactsTable.selectAll().where {
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            // Update contact (only non-null fields)
            // Addresses are managed separately via ContactAddressRepository
            ContactsTable.update({
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }) {
                request.name?.let { value -> it[name] = value.value }
                request.email?.let { value -> it[email] = value.value }
                request.iban?.let { value -> it[iban] = value.value }
                request.phone?.let { value -> it[phone] = value.value }
                request.vatNumber?.let { value -> it[vatNumber] = value.value }
                // contactType removed - roles are derived from cashflow items
                request.businessType?.let { value -> it[businessType] = value }
                request.contactPerson?.let { value -> it[contactPerson] = value }
                request.companyNumber?.let { value -> it[companyNumber] = value }
                request.defaultPaymentTerms?.let { value -> it[defaultPaymentTerms] = value }
                request.defaultVatRate?.let { value -> it[defaultVatRate] = BigDecimal(value) }
                // NOTE: peppolId/peppolEnabled removed - PEPPOL status is now in PeppolDirectoryCacheTable
                request.tags?.let { value -> it[tags] = value }
                request.isActive?.let { value -> it[isActive] = value }
            }

            // Fetch and return the updated contact (addresses populated by caller)
            ContactsTable.selectAll().where {
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.single().let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    // NOTE: updateContactPeppol() removed - PEPPOL status is now managed by PeppolDirectoryCacheRepository

    /**
     * Delete a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = ContactsTable.deleteWhere {
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Soft-delete (deactivate) a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deactivateContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = ContactsTable.update({
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }) {
                it[isActive] = false
            }
            updatedRows > 0
        }
    }

    /**
     * Reactivate a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun reactivateContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = ContactsTable.update({
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }) {
                it[isActive] = true
            }
            updatedRows > 0
        }
    }

    /**
     * Check if a contact exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            ContactsTable.selectAll().where {
                (ContactsTable.id eq Uuid.parse(contactId.toString())) and
                    (ContactsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.count() > 0
        }
    }

    /**
     * Get contact statistics for dashboard
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getContactStats(tenantId: TenantId): Result<ContactStats> = runCatching {
        dbQuery {
            val allContacts = ContactsTable.selectAll().where {
                ContactsTable.tenantId eq Uuid.parse(tenantId.toString())
            }

            val totalContacts = allContacts.count()
            val activeContacts = allContacts.copy().andWhere { ContactsTable.isActive eq true }.count()
            val inactiveContacts = totalContacts - activeContacts
            // NOTE: peppolEnabledContacts removed - PEPPOL status is now in PeppolDirectoryCacheTable

            // TODO: customer/vendor/both counts require JOIN with cashflow tables to derive from actual usage
            // For now, return 0 - these will be computed from derived roles
            val customerCount = 0L
            val vendorCount = 0L
            val bothCount = 0L

            ContactStats(
                totalContacts = totalContacts,
                activeContacts = activeContacts,
                inactiveContacts = inactiveContacts,
                customerCount = customerCount,
                vendorCount = vendorCount,
                bothCount = bothCount
            )
        }
    }

    // NOTE: listPeppolEnabledContacts() removed - PEPPOL status is now in PeppolDirectoryCacheTable

    // =========================================================================
    // CONTACT MATCHING (for AI document processing)
    // =========================================================================

    /**
     * Find a contact by VAT number (case-insensitive, normalized)
     * Returns the first active match.
     */
    suspend fun findByVatNumber(
        tenantId: TenantId,
        vatNumber: String
    ): Result<ContactDto?> = runCatching {
        dbQuery {
            val normalized = VatNumber.normalize(vatNumber)
            // Search for both normalized and original format
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                    (ContactsTable.isActive eq true)
            }.filter { row ->
                // Case-insensitive comparison on the result
                val storedVat = row[ContactsTable.vatNumber]?.let { VatNumber.normalize(it) }
                storedVat == normalized
            }.firstOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    // NOTE: findByPeppolId() removed - PEPPOL participant ID is now in PeppolDirectoryCacheTable

    /**
     * Find a contact by company number (exact match)
     * Returns the first active match.
     */
    suspend fun findByCompanyNumber(
        tenantId: TenantId,
        companyNumber: String
    ): Result<ContactDto?> = runCatching {
        dbQuery {
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                    (ContactsTable.companyNumber eq companyNumber) and
                    (ContactsTable.isActive eq true)
            }.singleOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * Find contacts by IBAN (exact match, normalized).
     * Returns active matches.
     */
    suspend fun findByIban(
        tenantId: TenantId,
        iban: Iban
    ): Result<List<ContactDto>> = runCatching {
        dbQuery {
            val normalized = Iban.from(iban.value)?.value ?: return@dbQuery emptyList()
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                    (ContactsTable.isActive eq true) and
                    (ContactsTable.iban eq normalized)
            }.map { row -> mapRowToContactDto(row) }
        }
    }

    /**
     * Find contacts by name (case-insensitive partial match).
     * Returns up to [limit] active matches sorted by name.
     *
     * Note: Country filtering removed - country is now in AddressTable.
     * TODO: Re-implement country filtering with JOIN to ContactAddressesTable/AddressTable if needed.
     */
    suspend fun findByName(
        tenantId: TenantId,
        name: String,
        limit: Int = 5
    ): Result<List<ContactDto>> = runCatching {
        dbQuery {
            val searchTerm = name.lowercase()
            val query = ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                    (ContactsTable.isActive eq true)
            }

            // Filter in-memory for case-insensitive matching and limit
            query.orderBy(ContactsTable.name to SortOrder.ASC)
                .filter { row ->
                    row[ContactsTable.name].lowercase().contains(searchTerm)
                }
                .take(limit)
                .map { row -> mapRowToContactDto(row) }
        }
    }

    /**
     * Get or create the "Unknown Contact" system placeholder for a tenant.
     * This contact is used when no match is found and user assigns to unknown.
     */
    suspend fun getOrCreateUnknownContact(tenantId: TenantId): Result<ContactDto> = runCatching {
        dbQuery {
            // Check if system contact already exists
            val existing = ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                    (ContactsTable.isSystemContact eq true)
            }.singleOrNull()

            if (existing != null) {
                mapRowToContactDto(existing)
            } else {
                // Create the Unknown Contact placeholder
                val contactId = ContactsTable.insertAndGetId {
                    it[ContactsTable.tenantId] = Uuid.parse(tenantId.toString())
                    it[ContactsTable.name] = "Unknown / Unassigned"
                    it[ContactsTable.isSystemContact] = true
                    it[ContactsTable.isActive] = true
                }

                ContactsTable.selectAll().where {
                    ContactsTable.id eq contactId.value
                }.single().let { row ->
                    mapRowToContactDto(row)
                }
            }
        }
    }

    // =========================================================================
    // ACTIVITY SUMMARY
    // =========================================================================

    /**
     * Get activity summary for a specific contact.
     * Returns counts and totals of invoices, inbound invoices, and expenses linked to this contact.
     *
     * CRITICAL: MUST filter by tenantId for multi-tenant isolation.
     */
    suspend fun getContactActivitySummary(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<ContactActivitySummary> = runCatching {
        dbQuery {
            val contactUuid = Uuid.parse(contactId.toString())
            val tenantUuid = Uuid.parse(tenantId.toString())

            // Get invoice count and total using simple queries
            val invoices = InvoicesTable.selectAll().where {
                (InvoicesTable.tenantId eq tenantUuid) and (InvoicesTable.contactId eq contactUuid)
            }.toList()

            val invoiceCount = invoices.size.toLong()
            val invoiceTotal = invoices.fold(BigDecimal.ZERO) { acc, row ->
                acc + (row[InvoicesTable.totalAmount] ?: BigDecimal.ZERO)
            }
            val invoiceLastDate = invoices.maxOfOrNull { it[InvoicesTable.createdAt] }

            // Get inbound invoice count and total
            val inboundInvoices = InvoicesTable.selectAll().where {
                (InvoicesTable.tenantId eq tenantUuid) and
                    (InvoicesTable.contactId eq contactUuid) and
                    (InvoicesTable.direction eq DocumentDirection.Inbound)
            }.toList()

            val inboundInvoiceCount = inboundInvoices.size.toLong()
            val inboundInvoiceTotal = inboundInvoices.fold(BigDecimal.ZERO) { acc, row ->
                acc + row[InvoicesTable.totalAmount]
            }
            val inboundInvoiceLastDate = inboundInvoices.maxOfOrNull { it[InvoicesTable.createdAt] }

            // Get expense count and total
            val expenses = ExpensesTable.selectAll().where {
                (ExpensesTable.tenantId eq tenantUuid) and (ExpensesTable.contactId eq contactUuid)
            }.toList()

            val expenseCount = expenses.size.toLong()
            val expenseTotal = expenses.fold(BigDecimal.ZERO) { acc, row ->
                acc + (row[ExpensesTable.amount] ?: BigDecimal.ZERO)
            }
            val expenseLastDate = expenses.maxOfOrNull { it[ExpensesTable.createdAt] }

            // Find the most recent activity date
            val lastActivityDate = listOfNotNull(invoiceLastDate, inboundInvoiceLastDate, expenseLastDate)
                .maxOrNull()

            // TODO: Count pending approval items (documents with this contact as suggested)
            val pendingApprovalCount = 0L

            ContactActivitySummary(
                contactId = contactId,
                invoiceCount = invoiceCount,
                invoiceTotal = invoiceTotal.toPlainString(),
                inboundInvoiceCount = inboundInvoiceCount,
                inboundInvoiceTotal = inboundInvoiceTotal.toPlainString(),
                expenseCount = expenseCount,
                expenseTotal = expenseTotal.toPlainString(),
                lastActivityDate = lastActivityDate,
                pendingApprovalCount = pendingApprovalCount
            )
        }
    }

    // =========================================================================
    // MERGE / DEDUPE
    // =========================================================================

    /**
     * Merge source contact into target contact.
     *
     * Process:
     * 1. Validate both contacts exist and belong to tenant
     * 2. Verify no VAT number conflict (both have different non-null VAT)
     * 3. Reassign all invoices, inbound invoices, expenses from source to target
     * 4. Move notes from source to target
     * 5. Add system note documenting the merge
     * 6. Soft-delete (deactivate) source contact
     *
     * CRITICAL: Must filter by tenantId for multi-tenant isolation.
     *
     * @param sourceContactId The contact to merge FROM (will be deactivated)
     * @param targetContactId The contact to merge INTO (will receive all items)
     * @param tenantId Tenant isolation
     * @param mergedByEmail Email of user performing the merge (for audit note)
     * @return MergeResult with counts of reassigned items
     */
    suspend fun mergeContacts(
        sourceContactId: ContactId,
        targetContactId: ContactId,
        tenantId: TenantId,
        mergedByEmail: String
    ): Result<ContactMergeResult> = runCatching {
        dbQuery {
            val sourceUuid = Uuid.parse(sourceContactId.toString())
            val targetUuid = Uuid.parse(targetContactId.toString())
            val tenantUuid = Uuid.parse(tenantId.toString())

            // 1. Fetch both contacts and validate
            val sourceContact = ContactsTable.selectAll().where {
                (ContactsTable.id eq sourceUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.singleOrNull() ?: throw IllegalArgumentException("Source contact not found")

            val targetContact = ContactsTable.selectAll().where {
                (ContactsTable.id eq targetUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.singleOrNull() ?: throw IllegalArgumentException("Target contact not found")

            // 2. Verify no VAT conflict
            val sourceVat = sourceContact[ContactsTable.vatNumber]
            val targetVat = targetContact[ContactsTable.vatNumber]
            if (!sourceVat.isNullOrBlank() && !targetVat.isNullOrBlank() && sourceVat != targetVat) {
                throw IllegalArgumentException(
                    "Cannot merge contacts with different VAT numbers: $sourceVat vs $targetVat"
                )
            }

            // 3. Check if source is a system contact
            if (sourceContact[ContactsTable.isSystemContact]) {
                throw IllegalArgumentException("Cannot merge system contact (Unknown / Unassigned)")
            }

            val sourceName = sourceContact[ContactsTable.name]

            // 4. Reassign invoices
            val invoicesReassigned = InvoicesTable.update({
                (InvoicesTable.tenantId eq tenantUuid) and
                    (InvoicesTable.contactId eq sourceUuid) and
                    (InvoicesTable.direction eq DocumentDirection.Outbound)
            }) {
                it[InvoicesTable.contactId] = targetUuid
            }

            // 5. Reassign inbound invoices
            val inboundInvoicesReassigned = InvoicesTable.update({
                (InvoicesTable.tenantId eq tenantUuid) and
                    (InvoicesTable.contactId eq sourceUuid) and
                    (InvoicesTable.direction eq DocumentDirection.Inbound)
            }) {
                it[InvoicesTable.contactId] = targetUuid
            }

            // 6. Reassign expenses
            val expensesReassigned = ExpensesTable.update({
                (ExpensesTable.tenantId eq tenantUuid) and (ExpensesTable.contactId eq sourceUuid)
            }) {
                it[ExpensesTable.contactId] = targetUuid
            }

            // 7. Move notes from source to target
            val notesReassigned = ContactNotesTable.update({
                (ContactNotesTable.tenantId eq tenantUuid) and (ContactNotesTable.contactId eq sourceUuid)
            }) {
                it[ContactNotesTable.contactId] = targetUuid
            }

            // 8. Add system note documenting the merge
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val mergeNote = buildString {
                appendLine("[SYSTEM] Contact merged from \"$sourceName\" (ID: $sourceContactId)")
                appendLine(
                    "- Reassigned: $invoicesReassigned invoices, $inboundInvoicesReassigned inbound invoices, $expensesReassigned expenses"
                )
                appendLine("- Notes moved: $notesReassigned")
                appendLine("- Merged by: $mergedByEmail at $now")
                appendLine("- Source contact archived")
            }

            ContactNotesTable.insert {
                it[ContactNotesTable.tenantId] = tenantUuid
                it[ContactNotesTable.contactId] = targetUuid
                it[content] = mergeNote
                it[authorName] = "System"
                it[createdAt] = now
                it[updatedAt] = now
            }

            // 9. Soft-delete (deactivate) source contact
            ContactsTable.update({
                (ContactsTable.id eq sourceUuid) and (ContactsTable.tenantId eq tenantUuid)
            }) {
                it[isActive] = false
            }

            ContactMergeResult(
                sourceContactId = sourceContactId,
                targetContactId = targetContactId,
                invoicesReassigned = invoicesReassigned,
                inboundInvoicesReassigned = inboundInvoicesReassigned,
                expensesReassigned = expensesReassigned,
                notesReassigned = notesReassigned,
                sourceArchived = true
            )
        }
    }

    // =========================================================================
    // MAPPING
    // =========================================================================

    /**
     * Map a database row to ContactDto.
     * Note: addresses list is empty - caller should populate via ContactAddressRepository.
     */
    private fun mapRowToContactDto(row: ResultRow): ContactDto {
        return ContactDto(
            id = ContactId.parse(row[ContactsTable.id].value.toString()),
            tenantId = TenantId.parse(row[ContactsTable.tenantId].toString()),
            name = Name(row[ContactsTable.name]),
            email = row[ContactsTable.email]?.let { Email(it) },
            iban = row[ContactsTable.iban]?.let { Iban(it) },
            vatNumber = row[ContactsTable.vatNumber]?.let { VatNumber(it) },
            businessType = row[ContactsTable.businessType],
            // Addresses are now in ContactAddressesTable, populated by caller
            contactPerson = row[ContactsTable.contactPerson],
            phone = row[ContactsTable.phone]?.let { PhoneNumber(it) },
            companyNumber = row[ContactsTable.companyNumber],
            defaultPaymentTerms = row[ContactsTable.defaultPaymentTerms],
            defaultVatRate = row[ContactsTable.defaultVatRate]?.let { VatRate.fromDbDecimal(it) },
            // NOTE: peppolId/peppolEnabled removed - PEPPOL status is now in PeppolDirectoryCacheTable
            tags = row[ContactsTable.tags],
            isActive = row[ContactsTable.isActive],
            createdAt = row[ContactsTable.createdAt],
            updatedAt = row[ContactsTable.updatedAt],
            // UI Contract fields
            isSystemContact = row[ContactsTable.isSystemContact],
            createdFromDocumentId = row[ContactsTable.createdFromDocumentId]?.let {
                DocumentId.parse(it.toString())
            },
            source = row[ContactsTable.contactSource]
            // addresses, derivedRoles and activitySummary are populated by service layer on demand
        )
    }
}
