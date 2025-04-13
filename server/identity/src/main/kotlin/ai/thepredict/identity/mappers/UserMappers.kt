package ai.thepredict.identity.mappers

import ai.thepredict.database.tables.UserEntity
import ai.thepredict.domain.model.User

val UserEntity.asUserApi: User
    get() = User(
        id = id.value.toString(),
        name = name,
        email = email,
        password = passwordHash
    )