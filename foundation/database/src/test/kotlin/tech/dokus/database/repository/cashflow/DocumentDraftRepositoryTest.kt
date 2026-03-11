package tech.dokus.database.repository.cashflow

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.utils.json
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class DocumentDraftRepositoryTest {

    private lateinit var database: Database
    private val repository = DocumentRepository()

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
                ContactsTable,
                DocumentBlobsTable,
                DocumentsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
            )
        }

        val tenantUuid = UUID.randomUUID()
        val documentUuid = UUID.randomUUID()
        val contactUuid = UUID.randomUUID()

        tenantId = TenantId(tenantUuid.toKotlinUuid())
        documentId = DocumentId(documentUuid.toKotlinUuid())

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

            // Set draft columns on the document row
            DocumentsTable.update({ DocumentsTable.id eq documentUuid }) {
                it[linkedContactId] = contactUuid
                it[linkedContactSource] = ContactLinkSource.AI
                it[matchEvidence] = json.encodeToString(
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
            exec("DROP ALL OBJECTS")
        }
    }

    @Test
    fun `updateContactResolution clears stale link and evidence when no valid link is passed`() = runBlocking {
        val updated = repository.updateContactResolution(
            documentId = documentId,
            tenantId = tenantId,
            counterpartySnapshot = null,
            counterparty = CounterpartyInfo.Unresolved()
        )

        assertTrue(updated)

        val draft = repository.getDraftByDocumentId(documentId, tenantId)
        assertNull(draft?.counterparty)
    }
}
