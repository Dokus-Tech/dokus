package tech.dokus.foundation.backend.database

import tech.dokus.domain.database.DbEnum
import org.jetbrains.exposed.v1.core.Table

inline fun <reified T> Table.dbEnumeration(
    name: String
) where T : Enum<T>, T : DbEnum = customEnumeration(
    name = name,
    sql = "VARCHAR(50)",
    fromDb = { value ->
        enumValues<T>().first { it.dbValue == value as String }
    },
    toDb = { it.dbValue }
)
