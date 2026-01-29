package tech.dokus.backend.services.documents

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ContactLinkDecisionType
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ContactEvidence
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedInvoiceFields
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ContactLinkingServiceTest {

    private lateinit var database: Database
    private lateinit var draftRepository: DocumentDraftRepository
    private lateinit var contactRepository: ContactRepository
    private var tenantId: TenantId = TenantId.generate()
    private var documentId: DocumentId = DocumentId.generate()
    private var runId: IngestionRunId = IngestionRunId.generate()
    private lateinit var tenantUuid: UUID
    private lateinit var documentUuid: UUID
    private lateinit var runUuid: UUID
    private val validVatA = "BE0428759497"
    private val validVatB = "BE0476795481"

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_contact_link_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                DocumentsTable,
                DocumentIngestionRunsTable,
                ContactsTable,
                DocumentDraftsTable
            )
        }

        tenantUuid = UUID.randomUUID()
        documentUuid = UUID.randomUUID()
        runUuid = UUID.randomUUID()

        tenantId = TenantId(tenantUuid.toKotlinUuid())
        documentId = DocumentId.parse(documentUuid.toString())
        runId = IngestionRunId.parse(runUuid.toString())

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Contact Link Test Tenant"
                it[displayName] = "Contact Link Test Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }

            DocumentsTable.insert {
                it[id] = documentUuid
                it[tenantId] = tenantUuid
                it[filename] = "invoice.pdf"
                it[contentType] = "application/pdf"
                it[sizeBytes] = 1234
                it[storageKey] = "documents/test/invoice.pdf"
                it[contentHash] = null
                it[documentSource] = DocumentSource.Upload
            }

            DocumentIngestionRunsTable.insert {
                it[id] = runUuid
                it[documentId] = documentUuid
                it[tenantId] = tenantUuid
                it[status] = IngestionStatus.Processing
                it[provider] = "test"
            }
        }

        draftRepository = DocumentDraftRepository()
        contactRepository = ContactRepository()
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                DocumentDraftsTable,
                ContactsTable,
                DocumentIngestionRunsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `VAT_ONLY auto-links when VAT matches`() = runBlocking {
        val counterpartyVat = validVatA
        val contactId = insertContact(counterpartyVat)
        createDraft(counterpartyVat)

        val service = ContactLinkingService(draftRepository, contactRepository)
        val updated = service.applyLinkDecision(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            extractedData = extraction(counterpartyVat),
            decisionType = ContactLinkDecisionType.AutoLink,
            contactId = contactId,
            decisionReason = "VAT match",
            decisionConfidence = null,
            evidence = ContactEvidence(ambiguityCount = 1)
        )

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertEquals(true, updated)
        assertEquals(contactId, draft?.linkedContactId)
        assertEquals(ContactLinkSource.AI, draft?.linkedContactSource)
        assertNull(draft?.suggestedContactId)
        assertEquals(true, draft?.contactEvidence?.vatMatched)
    }

    @Test
    fun `VAT_ONLY suggests when VAT mismatches`() = runBlocking {
        val counterpartyVat = validVatA
        val contactId = insertContact(validVatB)
        createDraft(counterpartyVat)

        val service = ContactLinkingService(draftRepository, contactRepository)
        service.applyLinkDecision(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            extractedData = extraction(counterpartyVat),
            decisionType = ContactLinkDecisionType.AutoLink,
            contactId = contactId,
            decisionReason = "VAT mismatch",
            decisionConfidence = 0.6f,
            evidence = ContactEvidence(ambiguityCount = 1)
        )

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertNull(draft?.linkedContactId)
        assertEquals(contactId, draft?.suggestedContactId)
        assertNotNull(draft?.contactEvidence)
        assertEquals(false, draft?.contactEvidence?.vatMatched)
    }

    @Test
    fun `USER-linked contact blocks AI override`() = runBlocking {
        val counterpartyVat = validVatA
        val userContactId = insertContact(counterpartyVat)
        createDraft(counterpartyVat)

        draftRepository.updateCounterparty(
            documentId = documentId,
            tenantId = tenantId,
            contactId = userContactId,
            intent = null,
            source = ContactLinkSource.User
        )

        val aiContactId = insertContact(validVatB)
        val service = ContactLinkingService(draftRepository, contactRepository)
        val updated = service.applyLinkDecision(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            extractedData = extraction(counterpartyVat),
            decisionType = ContactLinkDecisionType.AutoLink,
            contactId = aiContactId,
            decisionReason = "AI decision",
            decisionConfidence = null,
            evidence = ContactEvidence(ambiguityCount = 1)
        )

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertEquals(false, updated)
        assertEquals(userContactId, draft?.linkedContactId)
        assertEquals(ContactLinkSource.User, draft?.linkedContactSource)
        assertNull(draft?.suggestedContactId)
    }

    @Test
    fun `VAT_OR_STRONG_SIGNALS auto-links on strong evidence`() = runBlocking {
        val counterpartyVat = validVatA
        val contactId = insertContact(validVatB)
        createDraft(counterpartyVat)

        val evidence = ContactEvidence(
            ibanMatched = true,
            addressMatched = true,
            nameSimilarity = 0.93,
            ambiguityCount = 1
        )

        val service = ContactLinkingService(draftRepository, contactRepository)
        service.applyLinkDecision(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            extractedData = extraction(counterpartyVat),
            decisionType = ContactLinkDecisionType.AutoLink,
            contactId = contactId,
            decisionReason = "Strong signals",
            decisionConfidence = null,
            evidence = evidence
        )

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertEquals(contactId, draft?.linkedContactId)
        assertEquals(ContactLinkSource.AI, draft?.linkedContactSource)
    }

    private fun extraction(vat: String): ExtractedDocumentData {
        return ExtractedDocumentData(
            documentType = DocumentType.Invoice,
            invoice = ExtractedInvoiceFields(
                clientVatNumber = vat,
                clientName = "Vendor"
            )
        )
    }

    private fun createDraft(counterpartyVat: String) {
        runBlocking {
            draftRepository.createOrUpdateFromIngestion(
                documentId = documentId,
                tenantId = tenantId,
                runId = runId,
                extractedData = extraction(counterpartyVat),
                documentType = DocumentType.Invoice
            )
        }
    }

    private fun insertContact(vat: String): ContactId {
        val contactUuid = UUID.randomUUID()
        transaction(database) {
            ContactsTable.insert {
                it[ContactsTable.id] = contactUuid
                it[ContactsTable.tenantId] = tenantUuid
                it[ContactsTable.name] = "Vendor $vat"
                it[ContactsTable.vatNumber] = vat
            }
        }
        return ContactId(contactUuid.toKotlinUuid())
    }
}
