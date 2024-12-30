@file:OptIn(ExperimentalUuidApi::class)

package ai.thepredict.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class User(
    val id: Id,
    val name: String,
    val email: String,
    val password: String,
) {
    @Serializable
    @JvmInline
    value class Id(@Contextual val value: Uuid)
}

@Serializable
data class NewUser(
    val name: String,
    val email: String,
    val password: String,
)
