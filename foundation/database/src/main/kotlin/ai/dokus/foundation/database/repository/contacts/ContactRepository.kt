package ai.dokus.foundation.database.repository.contacts

import ai.dokus.foundation.database.tables.contacts.ContactsTable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ContactType
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactStats
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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
                it[contactType] = request.contactType
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
     */
    suspend fun listContacts(
        tenantId: TenantId,
        contactType: ContactType? = null,
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

            // Apply filters
            if (contactType != null) {
                query = query.andWhere { ContactsTable.contactType eq contactType }
            }
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
     * List customers only (ContactType.Customer or ContactType.Both)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listCustomers(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        dbQuery {
            var query = ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                ((ContactsTable.contactType eq ContactType.Customer) or (ContactsTable.contactType eq ContactType.Both))
            }

            if (isActive != null) {
                query = query.andWhere { ContactsTable.isActive eq isActive }
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
     * List vendors only (ContactType.Vendor or ContactType.Both)
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listVendors(
        tenantId: TenantId,
        isActive: Boolean? = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactDto>> = runCatching {
        dbQuery {
            var query = ContactsTable.selectAll().where {
                (ContactsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                ((ContactsTable.contactType eq ContactType.Vendor) or (ContactsTable.contactType eq ContactType.Both))
            }

            if (isActive != null) {
                query = query.andWhere { ContactsTable.isActive eq isActive }
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
                request.contactType?.let { value -> it[contactType] = value }
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
            val customerCount = allContacts.copy().andWhere {
                (ContactsTable.contactType eq ContactType.Customer) or (ContactsTable.contactType eq ContactType.Both)
            }.count()
            val vendorCount = allContacts.copy().andWhere {
                (ContactsTable.contactType eq ContactType.Vendor) or (ContactsTable.contactType eq ContactType.Both)
            }.count()
            val bothCount = allContacts.copy().andWhere { ContactsTable.contactType eq ContactType.Both }.count()

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
            contactType = row[ContactsTable.contactType],
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
            updatedAt = row[ContactsTable.updatedAt]
        )
    }
}
