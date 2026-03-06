package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object FirmsTable : UUIDTable("firms") {
    val name = varchar("name", 255)
    val vatNumber = varchar("vat_number", 50)
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(vatNumber)
    }
}
