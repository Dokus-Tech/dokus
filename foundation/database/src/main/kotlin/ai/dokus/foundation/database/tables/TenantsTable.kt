package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.*
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Root entity representing each customer account (freelancer or company)
 * One tenant = one paying customer
 */
object TenantsTable : UUIDTable("tenants") {
    // Identity
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()

    // Subscription
    val plan = tenantPlanEnumeration("plan")
    val status = tenantStatusEnumeration("status").default(TenantStatus.ACTIVE)
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()

    // Localization
    val country = varchar("country", 2).default("BE") // ISO 3166-1 alpha-2
    val language = languageEnumeration("language").default(Language.EN)

    // Business info
    val vatNumber = varchar("vat_number", 50).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, email)
        index(false, status)
    }
}