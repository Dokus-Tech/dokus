package ai.thepredict.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.random.Random

@Serializable
data class Workspace(
    val id: Id,
    val name: String,
    val legalName: String? = null,
    val url: String? = null,
) {
    @Serializable
    @JvmInline
    value class Id(val value: Int) {
        companion object {
            val random: Id get() = Id(Random.nextInt().absoluteValue)
        }
    }
}