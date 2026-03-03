package tech.dokus.features.cashflow.presentation.documents.route

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
}
