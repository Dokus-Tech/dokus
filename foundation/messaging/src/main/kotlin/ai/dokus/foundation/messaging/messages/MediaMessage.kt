package ai.dokus.foundation.messaging.messages

import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.OrganizationId
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
 * Message emitted when a media file is uploaded and queued for processing.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class MediaProcessingRequestedMessage(
    override val messageId: MessageId,
    override val timestamp: Instant,
    override val sourceService: String,
    val mediaId: MediaId,
    val organizationId: OrganizationId,
    val storageKey: String,
    val storageBucket: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long
) : Message {
    override val messageType: String = "media.processing.requested"

    companion object {
        val channelName = ChannelName("media.processing")

        fun routingKey(): RoutingKey = RoutingKey("media.processing.requested")

        fun create(
            mediaId: MediaId,
            organizationId: OrganizationId,
            storageKey: String,
            storageBucket: String,
            filename: String,
            mimeType: String,
            sizeBytes: Long,
            sourceService: String = "media-service"
        ): MediaProcessingRequestedMessage = MediaProcessingRequestedMessage(
            messageId = MessageId(Uuid.random().toString()),
            timestamp = Clock.System.now(),
            sourceService = sourceService,
            mediaId = mediaId,
            organizationId = organizationId,
            storageKey = storageKey,
            storageBucket = storageBucket,
            filename = filename,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }
}
