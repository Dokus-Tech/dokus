package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : UUIDTable("users") {
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val matricule = varchar("matricule", 50).nullable()
    val email = varchar("email", 255).uniqueIndex()
    val mobilePhone = varchar("mobile_phone", 50).nullable()
    val officePhone = varchar("office_phone", 50).nullable()

    val passwordHash = varchar("password_hash", 255)
    val lastPasswordChange = timestamp("last_password_change").nullable()
    val passwordExpiresAt = timestamp("password_expires_at").nullable()

    val unitCode = varchar("unit_code", 100).nullable()
    val department = varchar("department", 100).nullable()
    val isOGP = bool("is_ogp").default(false)
    val isOBP = bool("is_obp").default(false)
    val radioCallSign = varchar("radio_call_sign", 50).nullable()
    val lockedUntil = timestamp("locked_until").nullable()
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lastLoginAt = timestamp("last_login_at").nullable()
    val lastActivityAt = timestamp("last_activity_at").nullable()

    val galopId = varchar("galop_id", 50).nullable()
    val astridId = varchar("astrid_id", 50).nullable()
    val adUsername = varchar("ad_username", 100).nullable()

    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val verifiedBy = uuid("verified_by").nullable()
    val lastModifiedBy = uuid("last_modified_by").nullable()
    val deactivatedAt = timestamp("deactivated_at").nullable()
    val deactivationReason = text("deactivation_reason").nullable()

    val dataRetentionExpiresAt = timestamp("data_retention_expires_at").nullable()
    val lastPrivacyReviewAt = timestamp("last_privacy_review_at").nullable()
    val gdprConsentAt = timestamp("gdpr_consent_at").nullable()

    val emergencyContactName = varchar("emergency_contact_name", 200).nullable()
    val emergencyContactPhone = varchar("emergency_contact_phone", 50).nullable()

    init {
        index("idx_user_unit", false, unitCode)
    }
}