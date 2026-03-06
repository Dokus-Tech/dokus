package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.domain.enums.FirmRole
import tech.dokus.foundation.backend.database.dbEnumeration

object FirmMembersTable : UUIDTable("firm_members") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val firmId = reference("firm_id", FirmsTable, onDelete = ReferenceOption.CASCADE).index()
    val role = dbEnumeration<FirmRole>("role")
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(userId, firmId)
        index(false, firmId, isActive)
    }
}
