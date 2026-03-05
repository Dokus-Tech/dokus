package tech.dokus.features.cashflow.presentation.documents.route

import tech.dokus.features.cashflow.presentation.cashflow.components.DroppedFile
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentsRouteTest {

    @Test
    fun `saved-state refresh false does not trigger refresh`() {
        var clearCalls = 0
        var refreshCalls = 0

        handleSavedStateDocumentsRefresh(
            refreshRequired = false,
            clearRefreshResult = { clearCalls += 1 },
            onRefreshRequested = { refreshCalls += 1 },
        )

        assertEquals(0, clearCalls)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `saved-state refresh true clears flag and triggers refresh once`() {
        var clearCalls = 0
        var refreshCalls = 0

        handleSavedStateDocumentsRefresh(
            refreshRequired = true,
            clearRefreshResult = { clearCalls += 1 },
            onRefreshRequested = { refreshCalls += 1 },
        )

        assertEquals(1, clearCalls)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `dropped files handler ignores empty payload`() {
        var acceptedCalls = 0
        var enqueueCalls = 0

        handleDroppedFiles(
            files = emptyList(),
            enqueue = {
                enqueueCalls += 1
                emptyList()
            },
            onAccepted = { acceptedCalls += 1 }
        )

        assertEquals(0, enqueueCalls)
        assertEquals(0, acceptedCalls)
    }

    @Test
    fun `dropped files handler enqueues and accepts non-empty payload`() {
        var acceptedCalls = 0
        var enqueueCalls = 0
        var capturedNames = emptyList<String>()
        val dropped = listOf(
            DroppedFile(
                name = "invoice-1.pdf",
                bytes = byteArrayOf(1, 2, 3),
                mimeType = "application/pdf"
            )
        )

        handleDroppedFiles(
            files = dropped,
            enqueue = { files ->
                enqueueCalls += 1
                capturedNames = files.map { it.name }
                listOf("task-1")
            },
            onAccepted = { acceptedCalls += 1 }
        )

        assertEquals(1, enqueueCalls)
        assertEquals(1, acceptedCalls)
        assertEquals(listOf("invoice-1.pdf"), capturedNames)
    }

    @Test
    fun `retry local upload dispatches to manager callback`() {
        val taskIds = mutableListOf<String>()

        dispatchRetryLocalUpload(
            taskId = "task-123",
            retryUpload = { taskIds += it }
        )

        assertEquals(listOf("task-123"), taskIds)
    }

    @Test
    fun `dismiss local upload dispatches to manager callback`() {
        val taskIds = mutableListOf<String>()

        dispatchDismissLocalUpload(
            taskId = "task-456",
            cancelUpload = { taskIds += it }
        )

        assertEquals(listOf("task-456"), taskIds)
    }
}
