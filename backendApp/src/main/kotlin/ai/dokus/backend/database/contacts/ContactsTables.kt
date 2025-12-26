package ai.dokus.backend.database.contacts

import ai.dokus.foundation.database.tables.contacts.ContactNotesTable
import ai.dokus.foundation.database.tables.contacts.ContactsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Contacts service table initializer.
 *
 * OWNER: contacts service
 * Tables owned by this service:
 * - ContactsTable (unified contacts - customers AND vendors)
 * - ContactNotesTable (notes with history)
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 * - UsersTable (auth service) - for note authors
 */
object ContactsTables {
    private val logger = LoggerFactory.getLogger(ContactsTables::class.java)

    /**
     * Initialize contacts-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing contacts tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                ContactsTable,
                ContactNotesTable
            )
        }

        logger.info("Contacts tables initialized successfully")
    }
}
