package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.TenantPlan
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Root entity representing each customer account (freelancer or company).
 * Country is stored in the Address table, not here.
 *
 * OWNER: auth service
 */
object TenantTable : UUIDTable("tenants") {
    // Identity
    val type = dbEnumeration<TenantType>("type")
    val legalName = varchar("legal_name", 255)
    val displayName = varchar("display_name", 255)

    // Subscription
    val plan = dbEnumeration<TenantPlan>("plan")
    val status = dbEnumeration<TenantStatus>("status").default(TenantStatus.Active).index()
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()

    // Localization
    val language = dbEnumeration<Language>("language")

    // Business info
    val vatNumber = varchar("vat_number", 50).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
