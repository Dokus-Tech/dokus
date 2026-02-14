package tech.dokus.backend.routes.cashflow

import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.IngestionStatus
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentRecordPolicyTest {

    @Test
    fun `isInboxLifecycle allows only queued and processing`() {
        assertTrue(isInboxLifecycle(IngestionStatus.Queued))
        assertTrue(isInboxLifecycle(IngestionStatus.Processing))

        assertFalse(isInboxLifecycle(IngestionStatus.Succeeded))
        assertFalse(isInboxLifecycle(IngestionStatus.Failed))
        assertFalse(isInboxLifecycle(null))
    }
}
