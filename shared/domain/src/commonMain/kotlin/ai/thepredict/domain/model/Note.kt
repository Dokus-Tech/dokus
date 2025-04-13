package ai.thepredict.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class Note(
    val id: Id,
    val text: String,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val createdBy: String, // TODO: User ID
) {
    @Serializable
    @JvmInline
    value class Id(val value: Int)
}

@Serializable
data class NewNote(
    val text: String,
)