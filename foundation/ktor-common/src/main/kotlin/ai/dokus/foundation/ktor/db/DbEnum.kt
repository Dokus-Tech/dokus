package be.police.pulse.foundation.ktor.db

import be.police.pulse.domain.db.DbEnum
import org.jetbrains.exposed.sql.Table

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