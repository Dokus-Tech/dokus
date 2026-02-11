package tech.dokus.backend.peppol

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.peppol.policy.DefaultDocumentConfirmationPolicy
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentConfirmationPolicyTest {

    private val policy = DefaultDocumentConfirmationPolicy()
    private val tenantId = TenantId.generate()
    private val emptyDraftData = InvoiceDraftData()

    @Test
    fun `PEPPOL source should auto-confirm`() = runBlocking {
        val result = policy.canAutoConfirm(
            source = DocumentSource.Peppol,
            draftData = emptyDraftData,
            tenantId = tenantId
        )

        assertTrue(result, "PEPPOL documents should be auto-confirmed")
    }

    @Test
    fun `MANUAL source should auto-confirm`() = runBlocking {
        val result = policy.canAutoConfirm(
            source = DocumentSource.Manual,
            draftData = emptyDraftData,
            tenantId = tenantId
        )

        assertTrue(result, "Manual documents should be auto-confirmed")
    }

    @Test
    fun `UPLOAD source should NOT auto-confirm`() = runBlocking {
        val result = policy.canAutoConfirm(
            source = DocumentSource.Upload,
            draftData = emptyDraftData,
            tenantId = tenantId
        )

        assertFalse(result, "Uploaded documents should require review")
    }

    @Test
    fun `EMAIL source should NOT auto-confirm`() = runBlocking {
        val result = policy.canAutoConfirm(
            source = DocumentSource.Email,
            draftData = emptyDraftData,
            tenantId = tenantId
        )

        assertFalse(result, "Email documents should require review")
    }
}
