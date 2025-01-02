@file:OptIn(ExperimentalUuidApi::class)

package ai.thepredict.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class User(
    private val _id: String,
    val name: String,
    val email: String,
    val password: String,
) {
    val id: Id get() = Id(Uuid.parse(_id))

    @Serializable
    @JvmInline
    value class Id(@Contextual val value: Uuid) {
        constructor(id: String) : this(Uuid.parse(id))
    }
}

@Serializable
data class NewUser(
    val name: String,
    private val _email: String,
    val password: String,
) {
    val email = _email.lowercase()
}
