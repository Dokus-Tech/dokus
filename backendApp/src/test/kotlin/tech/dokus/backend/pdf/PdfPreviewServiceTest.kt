package tech.dokus.backend.pdf

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.pdf.PdfPreviewService
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.storage.ObjectStorage
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Unit tests for PdfPreviewService.
 *
 * Tests focus on:
 * - DPI clamping (72-300)
 * - MaxPages clamping (1-50)
 * - Cache key generation format
 * - Page bounds validation
 */
@OptIn(ExperimentalUuidApi::class)
class PdfPreviewServiceTest {

    private lateinit var objectStorage: ObjectStorage
    private lateinit var documentStorage: DocumentStorageService
    private lateinit var service: PdfPreviewService

    @BeforeEach
    fun setup() {
        objectStorage = mockk()
        documentStorage = mockk()
        service = PdfPreviewService(objectStorage, documentStorage)
    }

    // =========================================================================
    // DPI Clamping Tests
    // =========================================================================

    @Test
    fun `clampDpi should clamp low values to minimum 72`() {
        assertEquals(72, service.clampDpi(50))
        assertEquals(72, service.clampDpi(0))
        assertEquals(72, service.clampDpi(-100))
        assertEquals(72, service.clampDpi(71))
    }

    @Test
    fun `clampDpi should clamp high values to maximum 300`() {
        assertEquals(300, service.clampDpi(400))
        assertEquals(300, service.clampDpi(1000))
        assertEquals(300, service.clampDpi(301))
    }

    @Test
    fun `clampDpi should keep values within range unchanged`() {
        assertEquals(72, service.clampDpi(72))
        assertEquals(150, service.clampDpi(150))
        assertEquals(300, service.clampDpi(300))
        assertEquals(200, service.clampDpi(200))
    }

    // =========================================================================
    // MaxPages Clamping Tests
    // =========================================================================

    @Test
    fun `clampMaxPages should clamp low values to minimum 1`() {
        assertEquals(1, service.clampMaxPages(0))
        assertEquals(1, service.clampMaxPages(-10))
    }

    @Test
    fun `clampMaxPages should clamp high values to maximum 50`() {
        assertEquals(50, service.clampMaxPages(100))
        assertEquals(50, service.clampMaxPages(51))
        assertEquals(50, service.clampMaxPages(1000))
    }

    @Test
    fun `clampMaxPages should keep values within range unchanged`() {
        assertEquals(1, service.clampMaxPages(1))
        assertEquals(10, service.clampMaxPages(10))
        assertEquals(50, service.clampMaxPages(50))
        assertEquals(25, service.clampMaxPages(25))
    }

    // =========================================================================
    // Cache Key Generation Tests
    // =========================================================================

    @Test
    fun `generateCacheKey should produce correct format`() {
        val tenantId = TenantId(Uuid.parse("11111111-1111-1111-1111-111111111111"))
        val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

        val key = service.generateCacheKey(tenantId, documentId, 150, 1)

        assertEquals(
            "pdf_previews/$tenantId/$documentId/dpi-150/page-1.png",
            key
        )
    }

    @Test
    fun `generateCacheKey should include all parameters`() {
        val tenantId = TenantId(Uuid.parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        val documentId = DocumentId.parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

        val key1 = service.generateCacheKey(tenantId, documentId, 72, 1)
        val key2 = service.generateCacheKey(tenantId, documentId, 300, 10)

        assertEquals(
            "pdf_previews/$tenantId/$documentId/dpi-72/page-1.png",
            key1
        )
        assertEquals(
            "pdf_previews/$tenantId/$documentId/dpi-300/page-10.png",
            key2
        )
    }

    @Test
    fun `generateCacheKey should produce unique keys for different dpi`() {
        val tenantId = TenantId(Uuid.parse("11111111-1111-1111-1111-111111111111"))
        val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

        val key72 = service.generateCacheKey(tenantId, documentId, 72, 1)
        val key150 = service.generateCacheKey(tenantId, documentId, 150, 1)
        val key300 = service.generateCacheKey(tenantId, documentId, 300, 1)

        // All keys should be unique
        val keys = setOf(key72, key150, key300)
        assertEquals(3, keys.size, "Cache keys should be unique for different DPI values")
    }

    @Test
    fun `generateCacheKey should produce unique keys for different pages`() {
        val tenantId = TenantId(Uuid.parse("11111111-1111-1111-1111-111111111111"))
        val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

        val keyPage1 = service.generateCacheKey(tenantId, documentId, 150, 1)
        val keyPage2 = service.generateCacheKey(tenantId, documentId, 150, 2)
        val keyPage3 = service.generateCacheKey(tenantId, documentId, 150, 3)

        // All keys should be unique
        val keys = setOf(keyPage1, keyPage2, keyPage3)
        assertEquals(3, keys.size, "Cache keys should be unique for different pages")
    }

    // =========================================================================
    // Page Bounds Validation Tests (via getPageImage mock)
    // =========================================================================

    @Test
    fun `getPageImage should reject page less than 1`() {
        val tenantId = TenantId(Uuid.parse("11111111-1111-1111-1111-111111111111"))
        val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

        // Mock cache miss
        coEvery { objectStorage.exists(any()) } returns false
        // Mock PDF download - return minimal PDF bytes (will fail to parse, but we expect page validation first)
        coEvery { documentStorage.downloadDocument(any()) } returns ByteArray(0)

        // Note: The actual page validation happens inside renderPage() which is private
        // For this test, we verify behavior through the public API
        // Page < 1 should cause an error when rendering
        val exception = assertFailsWith<IllegalArgumentException> {
            runBlocking {
                service.getPageImage(tenantId, documentId, "storage/key", 0, 150)
            }
        }

        assertEquals("Page number must be >= 1, got: 0", exception.message)
    }
}
