package ai.thepredict.data

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val text: String,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val createdBy: String, // TODO: User ID
)