package ai.dokus.foundation.messaging.messages

import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.messaging.core.ChannelName
import ai.dokus.foundation.messaging.core.Message
import ai.dokus.foundation.messaging.core.MessageId
import ai.dokus.foundation.messaging.core.RoutingKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Message emitted when a document is uploaded and queued for AI extraction processing.
 *
 * The document processor worker consumes this message to:
 * 1. Fetch the document from MinIO
 * 2. Run AI extraction (Koog/OpenAI/Anthropic)
 * 3. Update the processing record with extracted data
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class DocumentProcessingRequestedMessage(
    override val messageId: MessageId,
    override val timestamp: Instant,
    override val sourceService: String,

    /** The document being processed */
    val documentId: DocumentId,

    /** The processing record ID */
    val processingId: DocumentProcessingId,

    /** Tenant for multi-tenant isolation */
    val tenantId: TenantId,

    /** MinIO storage key for fetching the file */
    val storageKey: String,

    /** Original filename */
    val filename: String,

    /** MIME type of the document */
    val mimeType: String,

    /** File size in bytes */
    val sizeBytes: Long
) : Message {
    override val messageType: String = "document.processing.requested"

    companion object {
        val channelName = ChannelName("document.processing")

        fun routingKey(): RoutingKey = RoutingKey("document.processing.requested")

        fun create(
            documentId: DocumentId,
            processingId: DocumentProcessingId,
            tenantId: TenantId,
            storageKey: String,
            filename: String,
            mimeType: String,
            sizeBytes: Long,
            sourceService: String = "cashflow-service"
        ): DocumentProcessingRequestedMessage = DocumentProcessingRequestedMessage(
            messageId = MessageId(Uuid.random().toString()),
            timestamp = Clock.System.now(),
            sourceService = sourceService,
            documentId = documentId,
            processingId = processingId,
            tenantId = tenantId,
            storageKey = storageKey,
            filename = filename,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }
}

/**
 * Message emitted when document processing completes (success or failure).
 * Can be used for notifications or triggering downstream workflows.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class DocumentProcessingCompletedMessage(
    override val messageId: MessageId,
    override val timestamp: Instant,
    override val sourceService: String,

    val documentId: DocumentId,
    val processingId: DocumentProcessingId,
    val tenantId: TenantId,

    /** Whether processing succeeded */
    val success: Boolean,

    /** Detected document type (if successful) */
    val documentType: String? = null,

    /** Overall confidence score (if successful) */
    val confidence: Double? = null,

    /** Error message (if failed) */
    val errorMessage: String? = null
) : Message {
    override val messageType: String = "document.processing.completed"

    companion object {
        val channelName = ChannelName("document.processing")

        fun routingKey(success: Boolean): RoutingKey =
            if (success) RoutingKey("document.processing.completed.success")
            else RoutingKey("document.processing.completed.failure")

        fun success(
            documentId: DocumentId,
            processingId: DocumentProcessingId,
            tenantId: TenantId,
            documentType: String,
            confidence: Double,
            sourceService: String = "processor-service"
        ): DocumentProcessingCompletedMessage = DocumentProcessingCompletedMessage(
            messageId = MessageId(Uuid.random().toString()),
            timestamp = Clock.System.now(),
            sourceService = sourceService,
            documentId = documentId,
            processingId = processingId,
            tenantId = tenantId,
            success = true,
            documentType = documentType,
            confidence = confidence
        )

        fun failure(
            documentId: DocumentId,
            processingId: DocumentProcessingId,
            tenantId: TenantId,
            errorMessage: String,
            sourceService: String = "processor-service"
        ): DocumentProcessingCompletedMessage = DocumentProcessingCompletedMessage(
            messageId = MessageId(Uuid.random().toString()),
            timestamp = Clock.System.now(),
            sourceService = sourceService,
            documentId = documentId,
            processingId = processingId,
            tenantId = tenantId,
            success = false,
            errorMessage = errorMessage
        )
    }
}
