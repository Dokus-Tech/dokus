package tech.dokus.backend.util

import java.sql.SQLException

private const val PostgresUniqueViolationSqlState = "23505"

fun Throwable.isUniqueViolation(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is SQLException && cause.sqlState == PostgresUniqueViolationSqlState) {
            return true
        }
        cause = cause.cause
    }
    return false
}

