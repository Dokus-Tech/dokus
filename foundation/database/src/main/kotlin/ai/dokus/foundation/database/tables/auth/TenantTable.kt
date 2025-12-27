package ai.dokus.foundation.database.tables.auth

import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.TenantType
import tech.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

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
