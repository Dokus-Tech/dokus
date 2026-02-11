package tech.dokus.backend.contacts

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.contacts.ContactMatchingService
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactMatchReason
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ContactMatchingServiceTest {

    private lateinit var database: Database
    private lateinit var contactMatchingService: ContactMatchingService
    private var tenantId: TenantId = TenantId.generate()
    private lateinit var tenantUuid: UUID

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                DocumentsTable,
                ContactsTable
            )
        }

        tenantUuid = UUID.randomUUID()
        tenantId = TenantId(tenantUuid.toKotlinUuid())

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Test Company"
                it[displayName] = "Test Company"
                it[plan] = SubscriptionTier.CoreFounder
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }
        }

        contactMatchingService = ContactMatchingService(ContactRepository())
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                ContactsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `vat match wins over name match`() = runBlocking {
        val vatContactId = insertContact(
            name = "Vat Corp",
            vatNumber = "BE0123456789"
        )
        insertContact(
            name = "Name Corp",
            vatNumber = null
        )

        val suggestion = contactMatchingService.findMatch(
            tenantId = tenantId,
            extracted = ContactMatchingService.ExtractedCounterparty(
                name = "Name Corp",
                vatNumber = " be 0123.456.789 "
            )
        ).getOrThrow()

        assertEquals(ContactMatchReason.VatNumber, suggestion.matchReason)
        assertNotNull(suggestion.contactId)
        assertEquals(ContactId(vatContactId.toKotlinUuid()), suggestion.contactId)
    }

    @Test
    fun `name and country match when vat missing`() = runBlocking {
        // Note: Country is no longer stored on ContactsTable (moved to address table).
        // The matching service uses country from extracted data to boost confidence
        // and set the match reason, but doesn't filter by it in the database.
        val contactId = insertContact(
            name = "Globex NV",
            vatNumber = null
        )

        val suggestion = contactMatchingService.findMatch(
            tenantId = tenantId,
            extracted = ContactMatchingService.ExtractedCounterparty(
                name = "Globex NV",
                country = "BE"
            )
        ).getOrThrow()

        assertEquals(ContactMatchReason.NameAndCountry, suggestion.matchReason)
        assertNotNull(suggestion.contactId)
        assertEquals(ContactId(contactId.toKotlinUuid()), suggestion.contactId)
    }

    private fun insertContact(
        name: String,
        vatNumber: String?
    ): UUID {
        return transaction(database) {
            ContactsTable.insertAndGetId {
                it[ContactsTable.tenantId] = tenantUuid
                it[ContactsTable.name] = name
                it[ContactsTable.vatNumber] = vatNumber
                it[ContactsTable.isActive] = true
            }.value
        }
    }
}
