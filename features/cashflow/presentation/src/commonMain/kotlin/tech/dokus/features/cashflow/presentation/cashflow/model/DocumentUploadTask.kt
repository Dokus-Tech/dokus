package tech.dokus.features.cashflow.presentation.cashflow.model

import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents the status of an upload task.
 */
enum class UploadStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED
}

/**
 * Represents a single document upload task with its current state.
 *
 * @property id Unique identifier for this upload task
 * @property fileName Original file name
 * @property fileSize Size in bytes
 * @property mimeType MIME type of the file
 * @property bytes Raw file content (kept for retry)
 * @property status Current upload status
 * @property progress Upload progress from 0.0 to 1.0
 * @property error Error message if upload failed
 * @property documentId ID of the uploaded document (set after success)
 * @property retryCount Number of retry attempts
 */
@OptIn(ExperimentalUuidApi::class)
data class DocumentUploadTask(
    val id: String = Uuid.random().toString(),
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val bytes: ByteArray,
    val status: UploadStatus = UploadStatus.PENDING,
    val progress: Float = 0f,
    val error: DokusException? = null,
    val documentId: DocumentId? = null,
    val sourceId: DocumentSourceId? = null,
    val intakeOutcome: DocumentIntakeOutcome? = null,
    val linkedDocumentId: DocumentId? = null,
    val matchReviewId: DocumentMatchReviewId? = null,
    val sourceCount: Int = 1,
    val retryCount: Int = 0
) {
    /**
     * Maximum number of automatic retry attempts.
     */
    companion object {
        const val MAX_AUTO_RETRIES = 2
        const val MAX_MANUAL_RETRIES = 5
    }

    /**
     * Whether this task can be manually retried by the user.
     */
    val canManualRetry: Boolean
        get() = status == UploadStatus.FAILED && retryCount < MAX_MANUAL_RETRIES

    /**
     * Whether this task can be automatically retried.
     */
    val canAutoRetry: Boolean
        get() = status == UploadStatus.FAILED && retryCount < MAX_AUTO_RETRIES

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DocumentUploadTask

        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (fileSize != other.fileSize) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (status != other.status) return false
        if (progress != other.progress) return false
        if (error != other.error) return false
        if (documentId != other.documentId) return false
        if (sourceId != other.sourceId) return false
        if (intakeOutcome != other.intakeOutcome) return false
        if (linkedDocumentId != other.linkedDocumentId) return false
        if (matchReviewId != other.matchReviewId) return false
        if (sourceCount != other.sourceCount) return false
        if (retryCount != other.retryCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + (documentId?.hashCode() ?: 0)
        result = 31 * result + (sourceId?.hashCode() ?: 0)
        result = 31 * result + (intakeOutcome?.hashCode() ?: 0)
        result = 31 * result + (linkedDocumentId?.hashCode() ?: 0)
        result = 31 * result + (matchReviewId?.hashCode() ?: 0)
        result = 31 * result + sourceCount
        result = 31 * result + retryCount
        return result
    }
}
