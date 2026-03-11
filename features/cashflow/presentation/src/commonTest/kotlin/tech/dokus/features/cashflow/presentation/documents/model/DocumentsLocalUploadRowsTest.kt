package tech.dokus.features.cashflow.presentation.documents.model

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.UploadStatus
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentsLocalUploadRowsTest {

    @Test
    fun `pending and uploading map to preparing and uploading rows`() {
        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(
                task("t-1", "one.pdf", UploadStatus.PENDING),
                task("t-2", "two.pdf", UploadStatus.UPLOADING),
            ),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList()
        )

        assertEquals(2, rows.size)
        assertEquals(listOf("t-2", "t-1"), rows.map { it.taskId })
        assertEquals(
            listOf(
                DocumentsLocalUploadRow.Status.Uploading,
                DocumentsLocalUploadRow.Status.PreparingDocument
            ),
            rows.map { it.status }
        )
    }

    @Test
    fun `failed task maps to failed row`() {
        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(task("t-1", "failed.pdf", UploadStatus.FAILED)),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList()
        )

        assertEquals(1, rows.size)
        assertEquals(DocumentsLocalUploadRow.Status.Failed, rows.single().status)
        assertEquals("failed.pdf", rows.single().fileName)
    }

    @Test
    fun `completed row remains until remote row arrives`() {
        val uploadedDocId = DocumentId.parse("00000000-0000-0000-0000-000000000111")
        val completedTask = task(
            id = "t-1",
            fileName = "uploaded.pdf",
            status = UploadStatus.COMPLETED,
            documentId = uploadedDocId
        )

        val beforeSync = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(completedTask),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList()
        )
        assertEquals(1, beforeSync.size)
        assertEquals(DocumentsLocalUploadRow.Status.ReadingDocument, beforeSync.single().status)

        val afterSync = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(completedTask),
            uploadedDocuments = emptyMap(),
            remoteDocuments = listOf(remoteRecord(uploadedDocId))
        )
        assertTrue(afterSync.isEmpty())
    }

    @Test
    fun `completed row can resolve document id from uploaded documents map`() {
        val uploadedDocId = DocumentId.parse("00000000-0000-0000-0000-000000000222")
        val taskWithoutDocId = task(
            id = "t-1",
            fileName = "uploaded.pdf",
            status = UploadStatus.COMPLETED,
            documentId = null
        )

        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(taskWithoutDocId),
            uploadedDocuments = mapOf("t-1" to document(uploadedDocId, "uploaded.pdf")),
            remoteDocuments = emptyList()
        )

        assertEquals(1, rows.size)
        assertEquals(uploadedDocId, rows.single().documentId)
    }

    @Test
    fun `confirmed filter hides all local rows`() {
        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.Confirmed,
            uploadTasks = listOf(
                task("t-1", "one.pdf", UploadStatus.PENDING),
                task("t-2", "two.pdf", UploadStatus.FAILED),
                task(
                    id = "t-3",
                    fileName = "three.pdf",
                    status = UploadStatus.COMPLETED,
                    documentId = DocumentId.parse("00000000-0000-0000-0000-000000000333")
                ),
            ),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList()
        )

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `needs attention keeps local rows visible while remote replacement missing`() {
        val syncedId = DocumentId.parse("00000000-0000-0000-0000-000000000444")
        val unsyncedId = DocumentId.parse("00000000-0000-0000-0000-000000000555")

        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.NeedsAttention,
            uploadTasks = listOf(
                task(
                    id = "synced",
                    fileName = "synced.pdf",
                    status = UploadStatus.COMPLETED,
                    documentId = syncedId
                ),
                task(
                    id = "unsynced",
                    fileName = "unsynced.pdf",
                    status = UploadStatus.COMPLETED,
                    documentId = unsyncedId
                ),
                task("pending", "pending.pdf", UploadStatus.PENDING)
            ),
            uploadedDocuments = emptyMap(),
            remoteDocuments = listOf(remoteRecord(syncedId))
        )

        assertEquals(listOf("pending", "unsynced"), rows.map { it.taskId })
    }

    @Test
    fun `needs attention hides completed local row when document is known remote`() {
        val syncedId = DocumentId.parse("00000000-0000-0000-0000-000000000666")

        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.NeedsAttention,
            uploadTasks = listOf(
                task(
                    id = "confirmed",
                    fileName = "confirmed.pdf",
                    status = UploadStatus.COMPLETED,
                    documentId = syncedId
                ),
                task("pending", "pending.pdf", UploadStatus.PENDING)
            ),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList(),
            knownRemoteDocumentIds = setOf(syncedId)
        )

        assertEquals(listOf("pending"), rows.map { it.taskId })
    }

    @Test
    fun `newer local rows are prepended first`() {
        val first = task("old", "old.pdf", UploadStatus.PENDING)
        val second = task("new", "new.pdf", UploadStatus.FAILED)

        val rows = buildDocumentsLocalUploadRows(
            filter = DocumentFilter.All,
            uploadTasks = listOf(first, second),
            uploadedDocuments = emptyMap(),
            remoteDocuments = emptyList()
        )

        assertEquals(listOf("new", "old"), rows.map { it.taskId })
    }

    private fun task(
        id: String,
        fileName: String,
        status: UploadStatus,
        documentId: DocumentId? = null
    ): DocumentUploadTask {
        return DocumentUploadTask(
            id = id,
            fileName = fileName,
            fileSize = 120L,
            mimeType = "application/pdf",
            bytes = byteArrayOf(1, 2, 3),
            status = status,
            documentId = documentId
        )
    }

    private fun remoteRecord(documentId: DocumentId): DocumentRecordDto {
        return DocumentRecordDto(
            document = document(documentId, "remote-$documentId.pdf"),
            draft = null,
            latestIngestion = null,
            confirmedEntity = null,
            cashflowEntryId = null,
            pendingMatchReview = null,
            sources = emptyList()
        )
    }

    private fun document(documentId: DocumentId, filename: String): DocumentDto {
        return DocumentDto(
            id = documentId,
            tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001"),
            filename = filename,
            contentType = "application/pdf",
            sizeBytes = 512,
            storageKey = "documents/$filename",
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = LocalDateTime(2026, 1, 1, 10, 0),
            downloadUrl = null
        )
    }
}
