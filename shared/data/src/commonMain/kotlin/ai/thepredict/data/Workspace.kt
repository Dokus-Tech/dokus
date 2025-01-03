package ai.thepredict.data

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.random.Random

@Serializable
data class Workspace(
    val id: Id,
    val name: String,
    val legalName: String? = null,
    val taxNumber: String? = null,
    val url: String? = null,
    val createdAt: LocalDateTime,
) {
    @Serializable
    @JvmInline
    value class Id(val value: Int) {
        companion object {
            val random: Id get() = Id(Random.nextInt().absoluteValue)
        }
    }
}