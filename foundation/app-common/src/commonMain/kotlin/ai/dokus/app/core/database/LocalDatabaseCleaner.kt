package ai.dokus.app.core.database

/**
 * Clears all local frontend databases.
 *
 * Implementations should remove every persisted table row used by the app
 * (SQLDelight caches, etc.) so that a logout leaves no local data behind.
 */
interface LocalDatabaseCleaner {
    /**
     * Clears all local databases. Returns failure if any wipe operation failed.
     */
    suspend fun clearAll(): Result<Unit>
}
