package ai.dokus.auth.backend.database

import ai.dokus.foundation.database.tables.auth.AddressTable
import ai.dokus.foundation.database.tables.auth.PasswordResetTokensTable
import ai.dokus.foundation.database.tables.auth.RefreshTokensTable
import ai.dokus.foundation.database.tables.auth.TenantMembersTable
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Auth service table initializer.
 *
 * OWNER: auth service
 * Tables owned by this service:
 * - TenantTable (base tenant/organization)
 * - TenantSettingsTable
 * - UsersTable
 * - TenantMembersTable
 * - RefreshTokensTable
 * - PasswordResetTokensTable
 * - AddressTable
 */
object AuthTables {
    private val logger = LoggerFactory.getLogger(AuthTables::class.java)

    /**
     * Initialize auth-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing auth tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                // Core auth tables - created in dependency order
                TenantTable,
                TenantSettingsTable,
                UsersTable,
                TenantMembersTable,
                RefreshTokensTable,
                PasswordResetTokensTable,
                AddressTable
            )
        }

        logger.info("Auth tables initialized successfully")
    }
}
