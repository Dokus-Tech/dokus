package tech.dokus.database.repository.cashflow
import kotlin.uuid.Uuid

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.utils.json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentDraftRepositoryTest {

    private lateinit var database: Database
    private val repository = DocumentDraftRepository()

    private var tenantId: TenantId = TenantId.generate()
    private var documentId: DocumentId = DocumentId.generate()

    @BeforeTest
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_document_draft_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                DocumentsTable,
                DocumentIngestionRunsTable,
                ContactsTable,
                DocumentDraftsTable
            )
        }

        val tenantUuid = Uuid.random()
        val documentUuid = Uuid.random()
        val contactUuid = Uuid.random()

        tenantId = TenantId(tenantUuid)
        documentId = DocumentId(documentUuid)

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Test Tenant"
                it[displayName] = "Test Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            DocumentsTable.insert {
                it[id] = documentUuid
                it[tenantId] = tenantUuid
                it[filename] = "invoice.pdf"
                it[contentType] = "application/pdf"
                it[sizeBytes] = 1234L
                it[storageKey] = "documents/$tenantUuid/invoice.pdf"
            }

            ContactsTable.insert {
                it[id] = contactUuid
                it[tenantId] = tenantUuid
                it[name] = "Linked Contact"
                it[isActive] = true
            }

            DocumentDraftsTable.insert {
                it[DocumentDraftsTable.documentId] = documentUuid
                it[DocumentDraftsTable.tenantId] = tenantUuid
                it[DocumentDraftsTable.linkedContactId] = contactUuid
                it[DocumentDraftsTable.linkedContactSource] = ContactLinkSource.AI
                it[DocumentDraftsTable.matchEvidence] = json.encodeToString(
                    MatchEvidence(
                        vatMatch = true,
                        ibanMatch = false,
                        ambiguityCount = 1
                    )
                )
            }
        }
    }

    @AfterTest
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                DocumentDraftsTable,
                ContactsTable,
                DocumentIngestionRunsTable,
                DocumentsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `updateContactResolution clears stale link and evidence when no valid link is passed`() = runBlocking {
        val updated = repository.updateContactResolution(
            documentId = documentId,
            tenantId = tenantId,
            contactSuggestions = emptyList(),
            counterpartySnapshot = null,
            matchEvidence = null,
            linkedContactId = null,
            linkedContactSource = null
        )

        assertTrue(updated)

        val draft = repository.getByDocumentId(documentId, tenantId)
        assertNull(draft?.linkedContactId)
        assertNull(draft?.linkedContactSource)
        assertNull(draft?.matchEvidence)
    }
}
