package ai.dokus.foundation.database.repository.contacts

import ai.dokus.foundation.database.tables.cashflow.BillsTable
import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.database.tables.contacts.ContactNotesTable
import ai.dokus.foundation.database.tables.contacts.ContactsTable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
// ContactType removed - roles are now derived from cashflow items
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactMergeResult
import ai.dokus.foundation.domain.model.ContactStats
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateContactRequest
import tech.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import java.util.UUID

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
     * Create a new contact
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createContact(
        tenantId: TenantId,
        request: CreateContactRequest
    ): Result<ContactDto> = runCatching {
        dbQuery {
            val contactId = ContactsTable.insertAndGetId {
                it[ContactsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[name] = request.name
                it[email] = request.email
                it[phone] = request.phone
                it[vatNumber] = request.vatNumber
                // contactType removed - roles are derived from cashflow items
                it[businessType] = request.businessType
                it[addressLine1] = request.addressLine1
                it[addressLine2] = request.addressLine2
                it[city] = request.city
                it[postalCode] = request.postalCode
                it[country] = request.country
                it[contactPerson] = request.contactPerson
                it[companyNumber] = request.companyNumber
                it[defaultPaymentTerms] = request.defaultPaymentTerms
                it[defaultVatRate] = request.defaultVatRate?.let { rate -> java.math.BigDecimal(rate) }
                it[peppolId] = request.peppolId
                it[peppolEnabled] = request.peppolEnabled
                it[tags] = request.tags
            }

            // Fetch and return the created contact
            ContactsTable.selectAll().where {
                (ContactsTable.id eq contactId.value) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
        peppolEnabled: Boolean? = null,
        searchQuery: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        dbQuery {
            var query = ContactsTable.selectAll().where {
                ContactsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters (contactType removed - roles are derived)
            if (isActive != null) {
                query = query.andWhere { ContactsTable.isActive eq isActive }
            }
            if (peppolEnabled != null) {
                query = query.andWhere { ContactsTable.peppolEnabled eq peppolEnabled }
            }
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
        listContacts(tenantId, isActive, null, null, limit, offset).getOrThrow()
    }

    /**
     * List vendors - contacts with incoming bills/expenses.
     * TODO: Implement proper derived role filtering with JOIN to BillsTable/ExpensesTable
     * For now, returns all active contacts (caller should filter by derived roles)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listVendors(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        // TODO: Proper implementation requires JOIN with BillsTable/ExpensesTable
        // For now, delegate to listContacts
        listContacts(tenantId, isActive, null, null, limit, offset).getOrThrow()
    }

    /**
     * Update a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateContact(
        contactId: ContactId,
        tenantId: TenantId,
        request: UpdateContactRequest
    ): Result<ContactDto> = runCatching {
        dbQuery {
            // Verify contact exists and belongs to tenant
            val exists = ContactsTable.selectAll().where {
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            // Update contact (only non-null fields)
            ContactsTable.update({
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                request.name?.let { value -> it[name] = value }
                request.email?.let { value -> it[email] = value }
                request.phone?.let { value -> it[phone] = value }
                request.vatNumber?.let { value -> it[vatNumber] = value }
                // contactType removed - roles are derived from cashflow items
                request.businessType?.let { value -> it[businessType] = value }
                request.addressLine1?.let { value -> it[addressLine1] = value }
                request.addressLine2?.let { value -> it[addressLine2] = value }
                request.city?.let { value -> it[city] = value }
                request.postalCode?.let { value -> it[postalCode] = value }
                request.country?.let { value -> it[country] = value }
                request.contactPerson?.let { value -> it[contactPerson] = value }
                request.companyNumber?.let { value -> it[companyNumber] = value }
                request.defaultPaymentTerms?.let { value -> it[defaultPaymentTerms] = value }
                request.defaultVatRate?.let { value -> it[defaultVatRate] = java.math.BigDecimal(value) }
                request.peppolId?.let { value -> it[peppolId] = value }
                request.peppolEnabled?.let { value -> it[peppolEnabled] = value }
                request.tags?.let { value -> it[tags] = value }
                request.isActive?.let { value -> it[isActive] = value }
            }

            // Fetch and return the updated contact
            ContactsTable.selectAll().where {
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * Update contact's Peppol settings
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateContactPeppol(
        contactId: ContactId,
        tenantId: TenantId,
        peppolId: String?,
        peppolEnabled: Boolean
    ): Result<ContactDto> = runCatching {
        dbQuery {
            // Verify contact exists and belongs to tenant
            val exists = ContactsTable.selectAll().where {
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            ContactsTable.update({
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[ContactsTable.peppolId] = peppolId
                it[ContactsTable.peppolEnabled] = peppolEnabled
            }

            // Fetch and return the updated contact
            ContactsTable.selectAll().where {
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                mapRowToContactDto(row)
            }
        }
    }

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
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                ContactsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            val totalContacts = allContacts.count()
            val activeContacts = allContacts.copy().andWhere { ContactsTable.isActive eq true }.count()
            val inactiveContacts = totalContacts - activeContacts
            val peppolEnabledContacts = allContacts.copy().andWhere { ContactsTable.peppolEnabled eq true }.count()

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
                bothCount = bothCount,
                peppolEnabledContacts = peppolEnabledContacts
            )
        }
    }

    /**
     * Get Peppol-enabled contacts for a tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listPeppolEnabledContacts(tenantId: TenantId): Result<List<ContactDto>> = runCatching {
        dbQuery {
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.peppolEnabled eq true) and
                (ContactsTable.peppolId.isNotNull())
            }.orderBy(ContactsTable.name to SortOrder.ASC)
                .map { row -> mapRowToContactDto(row) }
        }
    }

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
            val normalized = vatNumber.uppercase().replace(" ", "").replace(".", "")
            // Search for both normalized and original format
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.isActive eq true)
            }.filter { row ->
                // Case-insensitive comparison on the result
                val storedVat = row[ContactsTable.vatNumber]?.uppercase()?.replace(" ", "")?.replace(".", "")
                storedVat == normalized
            }.firstOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * Find a contact by Peppol ID (exact match)
     * Returns the first active match.
     */
    suspend fun findByPeppolId(
        tenantId: TenantId,
        peppolId: String
    ): Result<ContactDto?> = runCatching {
        dbQuery {
            ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.peppolId eq peppolId) and
                (ContactsTable.isActive eq true)
            }.singleOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

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
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.companyNumber eq companyNumber) and
                (ContactsTable.isActive eq true)
            }.singleOrNull()?.let { row ->
                mapRowToContactDto(row)
            }
        }
    }

    /**
     * Find contacts by name (case-insensitive partial match)
     * Returns up to [limit] active matches sorted by name.
     */
    suspend fun findByName(
        tenantId: TenantId,
        name: String,
        country: String? = null,
        limit: Int = 5
    ): Result<List<ContactDto>> = runCatching {
        dbQuery {
            val searchTerm = "%${name.lowercase()}%"
            var query = ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.name like searchTerm) and
                (ContactsTable.isActive eq true)
            }

            if (country != null) {
                query = query.andWhere {
                    ContactsTable.country eq country.uppercase()
                }
            }

            // Filter in-memory for case-insensitive matching and limit
            query.orderBy(ContactsTable.name to SortOrder.ASC)
                .filter { row ->
                    row[ContactsTable.name].lowercase().contains(name.lowercase())
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
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ContactsTable.isSystemContact eq true)
            }.singleOrNull()

            if (existing != null) {
                mapRowToContactDto(existing)
            } else {
                // Create the Unknown Contact placeholder
                val contactId = ContactsTable.insertAndGetId {
                    it[ContactsTable.tenantId] = UUID.fromString(tenantId.toString())
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
     * Returns counts and totals of invoices, bills, and expenses linked to this contact.
     *
     * CRITICAL: MUST filter by tenantId for multi-tenant isolation.
     */
    suspend fun getContactActivitySummary(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<ContactActivitySummary> = runCatching {
        dbQuery {
            val contactUuid = UUID.fromString(contactId.toString())
            val tenantUuid = UUID.fromString(tenantId.toString())

            // Get invoice count and total using simple queries
            val invoices = InvoicesTable.selectAll().where {
                (InvoicesTable.tenantId eq tenantUuid) and (InvoicesTable.contactId eq contactUuid)
            }.toList()

            val invoiceCount = invoices.size.toLong()
            val invoiceTotal = invoices.fold(BigDecimal.ZERO) { acc, row ->
                acc + (row[InvoicesTable.totalAmount] ?: BigDecimal.ZERO)
            }
            val invoiceLastDate = invoices.maxOfOrNull { it[InvoicesTable.createdAt] }

            // Get bill count and total
            val bills = BillsTable.selectAll().where {
                (BillsTable.tenantId eq tenantUuid) and (BillsTable.contactId eq contactUuid)
            }.toList()

            val billCount = bills.size.toLong()
            val billTotal = bills.fold(BigDecimal.ZERO) { acc, row ->
                acc + (row[BillsTable.amount] ?: BigDecimal.ZERO)
            }
            val billLastDate = bills.maxOfOrNull { it[BillsTable.createdAt] }

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
            val lastActivityDate = listOfNotNull(invoiceLastDate, billLastDate, expenseLastDate)
                .maxOrNull()

            // TODO: Count pending approval items (documents with this contact as suggested)
            val pendingApprovalCount = 0L

            ContactActivitySummary(
                contactId = contactId,
                invoiceCount = invoiceCount,
                invoiceTotal = invoiceTotal.toPlainString(),
                billCount = billCount,
                billTotal = billTotal.toPlainString(),
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
     * 3. Reassign all invoices, bills, expenses from source to target
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
            val sourceUuid = UUID.fromString(sourceContactId.toString())
            val targetUuid = UUID.fromString(targetContactId.toString())
            val tenantUuid = UUID.fromString(tenantId.toString())

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
                throw IllegalArgumentException("Cannot merge contacts with different VAT numbers: $sourceVat vs $targetVat")
            }

            // 3. Check if source is a system contact
            if (sourceContact[ContactsTable.isSystemContact]) {
                throw IllegalArgumentException("Cannot merge system contact (Unknown / Unassigned)")
            }

            val sourceName = sourceContact[ContactsTable.name]

            // 4. Reassign invoices
            val invoicesReassigned = InvoicesTable.update({
                (InvoicesTable.tenantId eq tenantUuid) and (InvoicesTable.contactId eq sourceUuid)
            }) {
                it[InvoicesTable.contactId] = targetUuid
            }

            // 5. Reassign bills
            val billsReassigned = BillsTable.update({
                (BillsTable.tenantId eq tenantUuid) and (BillsTable.contactId eq sourceUuid)
            }) {
                it[BillsTable.contactId] = targetUuid
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
                appendLine("- Reassigned: $invoicesReassigned invoices, $billsReassigned bills, $expensesReassigned expenses")
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
                billsReassigned = billsReassigned,
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
     * Map a database row to ContactDto
     */
    private fun mapRowToContactDto(row: ResultRow): ContactDto {
        return ContactDto(
            id = ContactId.parse(row[ContactsTable.id].value.toString()),
            tenantId = TenantId.parse(row[ContactsTable.tenantId].toString()),
            name = Name(row[ContactsTable.name]),
            email = row[ContactsTable.email]?.let { Email(it) },
            vatNumber = row[ContactsTable.vatNumber]?.let { VatNumber(it) },
            businessType = row[ContactsTable.businessType],
            addressLine1 = row[ContactsTable.addressLine1],
            addressLine2 = row[ContactsTable.addressLine2],
            city = row[ContactsTable.city],
            postalCode = row[ContactsTable.postalCode],
            country = row[ContactsTable.country],
            contactPerson = row[ContactsTable.contactPerson],
            phone = row[ContactsTable.phone],
            companyNumber = row[ContactsTable.companyNumber],
            defaultPaymentTerms = row[ContactsTable.defaultPaymentTerms],
            defaultVatRate = row[ContactsTable.defaultVatRate]?.let { VatRate(it.toString()) },
            peppolId = row[ContactsTable.peppolId],
            peppolEnabled = row[ContactsTable.peppolEnabled],
            tags = row[ContactsTable.tags],
            isActive = row[ContactsTable.isActive],
            createdAt = row[ContactsTable.createdAt],
            updatedAt = row[ContactsTable.updatedAt],
            // UI Contract fields
            isSystemContact = row[ContactsTable.isSystemContact],
            createdFromDocumentId = row[ContactsTable.createdFromDocumentId]?.let {
                DocumentId.parse(it.toString())
            }
            // derivedRoles and activitySummary are populated by service layer on demand
        )
    }
}
