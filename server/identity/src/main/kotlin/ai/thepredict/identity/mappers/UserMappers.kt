package ai.thepredict.identity.mappers

import ai.thepredict.database.tables.UserEntity
import ai.thepredict.domain.User

val UserEntity.asUserApi: User
    get() = User(
        _id = id.value.toString(),
        name = name,
        email = email,
        password = passwordHash
    )