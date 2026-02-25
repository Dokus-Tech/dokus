package tech.dokus.database.tables.search

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.model.SearchSignalEventType

object SearchSignalStatsTable : UUIDTable("search_signal_stats") {
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE).index()
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val signalType = enumerationByName("signal_type", 40, SearchSignalEventType::class)
    val normalizedText = varchar("normalized_text", length = 80)
    val displayText = varchar("display_text", length = 120)
    val count = long("count").default(1L)
    val lastSeenAt = datetime("last_seen_at").defaultExpression(CurrentDateTime)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, userId, signalType, normalizedText)
        index(false, tenantId, userId, lastSeenAt)
        index(false, tenantId, userId, normalizedText)
    }
}
