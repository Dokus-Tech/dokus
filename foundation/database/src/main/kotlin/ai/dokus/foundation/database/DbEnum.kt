package ai.dokus.foundation.database

import ai.dokus.foundation.domain.database.DbEnum
import org.jetbrains.exposed.sql.Table
import kotlin.collections.first

inline fun <reified T> Table.dbEnumeration(
    name: String
) where T : Enum<T>, T : DbEnum = Table.customEnumeration(
    name = name,
    sql = "VARCHAR(50)",
    fromDb = { value ->
        enumValues<T>().first { it.dbValue == value as String }
    },
    toDb = { it.dbValue }
)