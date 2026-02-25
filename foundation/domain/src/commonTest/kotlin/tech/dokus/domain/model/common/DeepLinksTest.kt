package tech.dokus.domain.model.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinksTest {

    @Test
    fun `extractShareImportBatchId returns batch id for valid share import link`() {
        val deepLink = DeepLink("dokus://share/import?batch=batch-123")

        val batchId = DeepLinks.extractShareImportBatchId(deepLink)

        assertEquals("batch-123", batchId)
    }

    @Test
    fun `extractShareImportBatchId returns null for missing batch query param`() {
        val deepLink = DeepLink("dokus://share/import?foo=bar")

        val batchId = DeepLinks.extractShareImportBatchId(deepLink)

        assertNull(batchId)
    }

    @Test
    fun `extractShareImportBatchId returns null for empty batch query param`() {
        val deepLink = DeepLink("dokus://share/import?batch=")

        val batchId = DeepLinks.extractShareImportBatchId(deepLink)

        assertNull(batchId)
    }

    @Test
    fun `extractShareImportBatchId returns null for non share-import path`() {
        val deepLink = DeepLink("dokus://connect?host=localhost&port=8080")

        val batchId = DeepLinks.extractShareImportBatchId(deepLink)

        assertNull(batchId)
    }

    @Test
    fun `extractDocumentReviewId returns id for document review deep link`() {
        val deepLink = DeepLink("dokus://documents/review?documentId=doc-123")

        val documentId = DeepLinks.extractDocumentReviewId(deepLink)

        assertEquals("doc-123", documentId)
    }

    @Test
    fun `extractDocumentReviewId supports legacy id query key`() {
        val deepLink = DeepLink("dokus://cashflow/document_review?id=legacy-doc")

        val documentId = DeepLinks.extractDocumentReviewId(deepLink)

        assertEquals("legacy-doc", documentId)
    }

    @Test
    fun `extractVerifyEmailToken returns token for absolute https verify email link`() {
        val deepLink = DeepLink("https://app.dokus.tech/auth/verify-email?token=verify-123")

        val token = DeepLinks.extractVerifyEmailToken(deepLink)

        assertEquals("verify-123", token)
    }

    @Test
    fun `extractVerifyEmailToken returns token for absolute http verify email link`() {
        val deepLink = DeepLink("http://localhost:8081/auth/verify-email?token=verify-123")

        val token = DeepLinks.extractVerifyEmailToken(deepLink)

        assertEquals("verify-123", token)
    }

    @Test
    fun `extractResetPasswordToken returns token for absolute http reset password link`() {
        val deepLink = DeepLink("http://localhost:8081/auth/reset-password?token=reset-123")

        val token = DeepLinks.extractResetPasswordToken(deepLink)

        assertEquals("reset-123", token)
    }

    @Test
    fun `extractVerifyEmailToken returns null when token query param is missing`() {
        val deepLink = DeepLink("https://app.dokus.tech/auth/verify-email")

        val token = DeepLinks.extractVerifyEmailToken(deepLink)

        assertNull(token)
    }

    @Test
    fun `extractVerifyEmailToken returns null for absolute http verify email link without token`() {
        val deepLink = DeepLink("http://localhost:8081/auth/verify-email")

        val token = DeepLinks.extractVerifyEmailToken(deepLink)

        assertNull(token)
    }
}
