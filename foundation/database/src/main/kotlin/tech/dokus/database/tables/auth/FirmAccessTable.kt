package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.domain.enums.FirmAccessStatus
import tech.dokus.foundation.backend.database.dbEnumeration

object FirmAccessTable : UUIDTable("firm_access") {
    val firmId = reference("firm_id", FirmsTable, onDelete = ReferenceOption.CASCADE).index()
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE).index()
    val status = dbEnumeration<FirmAccessStatus>("status")
    val grantedByUserId = reference("granted_by_user_id", UsersTable, onDelete = ReferenceOption.NO_ACTION)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(firmId, tenantId)
        index(false, firmId, status)
        index(false, tenantId, status)
    }
}
