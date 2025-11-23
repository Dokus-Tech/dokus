package ai.dokus.auth.backend.database.tables

import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Root entity representing each customer account (freelancer or company)
 */
object OrganizationTable : UUIDTable("organizations") {
    // Identity
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()

    // Subscription
    val plan = dbEnumeration<OrganizationPlan>("plan")
    val status = dbEnumeration<TenantStatus>("status").default(TenantStatus.Active)
    val trialEndsAt = datetime("trial_ends_at").nullable()
    val subscriptionStartedAt = datetime("subscription_started_at").nullable()

    // Localization
    val country = dbEnumeration<Country>("country")
    val language = dbEnumeration<Language>("language")

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