package ai.thepredict.identity.mappers

import ai.thepredict.database.tables.UserEntity
import ai.thepredict.domain.User
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
val UserEntity.asUserApi: User
    get() = User(
        id = User.Id(userId.toKotlinUuid()),
        name = name,
        email = email,
        password = passwordHash
    )