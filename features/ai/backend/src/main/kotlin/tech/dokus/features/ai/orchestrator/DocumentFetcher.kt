package tech.dokus.features.ai.orchestrator

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

fun interface DocumentFetcher {
    suspend operator fun invoke(tenantId: TenantId, documentId: DocumentId): Result<FetchedDocumentData>

    /**
     * Document data fetched from storage.
     */
    data class FetchedDocumentData(
        val bytes: ByteArray,
        val mimeType: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FetchedDocumentData) return false
            return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }
}
