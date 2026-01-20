package tech.dokus.domain.repository

import tech.dokus.domain.ids.ExampleId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentExample

/**
 * Repository for document examples used in few-shot learning.
 *
 * When processing documents, the orchestrator looks up examples from the same vendor
 * to guide extraction. This improves accuracy for repeat vendors.
 *
 * CRITICAL: All implementations MUST filter by tenantId for multi-tenant security.
 */
interface ExampleRepository {

    /**
     * Find an example by vendor VAT number (exact match).
     *
     * This is the primary lookup method.
     *
     * @param tenantId The tenant to filter by (REQUIRED)
     * @param vatNumber The vendor VAT number to match
     * @return The example if found, null otherwise
     */
    suspend fun findByVendorVat(tenantId: TenantId, vatNumber: String): DocumentExample?

    /**
     * Find an example by vendor name (fuzzy match).
     *
     * This is the fallback lookup when VAT number is not available.
     * Uses trigram similarity for fuzzy matching.
     *
     * @param tenantId The tenant to filter by (REQUIRED)
     * @param name The vendor name to match
     * @param similarity Minimum similarity threshold (0.0 - 1.0, default: 0.85)
     * @return The example if found with sufficient similarity, null otherwise
     */
    suspend fun findByVendorName(
        tenantId: TenantId,
        name: String,
        similarity: Float = 0.85f
    ): DocumentExample?

    /**
     * Save a new example or update an existing one.
     *
     * If an example already exists for this vendor (by VAT), it will be updated.
     *
     * @param example The example to save
     * @return The saved example with generated ID
     */
    suspend fun save(example: DocumentExample): DocumentExample

    /**
     * Increment the usage count for an example.
     *
     * Called when an example is used for few-shot learning.
     *
     * @param exampleId The example ID
     */
    suspend fun incrementUsage(exampleId: ExampleId)

    /**
     * Delete an example.
     *
     * @param tenantId The tenant to filter by (REQUIRED)
     * @param exampleId The example ID to delete
     * @return true if deleted, false if not found
     */
    suspend fun delete(tenantId: TenantId, exampleId: ExampleId): Boolean

    /**
     * Count examples for a tenant.
     *
     * @param tenantId The tenant to filter by (REQUIRED)
     * @return Total number of examples
     */
    suspend fun countForTenant(tenantId: TenantId): Long
}
