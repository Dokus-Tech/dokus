package ai.thepredict.domain.model.old

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.random.Random

@Serializable
data class Contact(
    val id: Id,
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val taxNumber: String? = null,
    val companyName: String? = null,
    val notes: List<Note> = emptyList(),
    val url: String? = null,
    val logo: String? = null,
) {
    @Serializable
    @JvmInline
    value class Id(val value: Int) {
        companion object {
            val random: Id = Id(Random.nextInt().absoluteValue)
        }
    }

    @Serializable
    enum class State(val key: String) {
        @SerialName(KEY_ACTIVE)
        Active("ACTIVE"),

        @SerialName(KEY_ARCHIVED)
        Archived("ARCHIVED");

        companion object {
            private const val KEY_ACTIVE = "ACTIVE"
            private const val KEY_ARCHIVED = "ARCHIVED"
            val default = Active

            fun fromKey(value: String, orDefault: State = default): State {
                return entries.find { it.key == value } ?: orDefault
            }
        }
    }
}

@Serializable
data class NewContact(
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val taxNumber: String? = null,
    val companyName: String? = null,
)