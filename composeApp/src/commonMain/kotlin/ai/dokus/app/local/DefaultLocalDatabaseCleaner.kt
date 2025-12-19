package ai.dokus.app.local

import ai.dokus.app.auth.database.AuthDatabase
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Clears every frontend SQLDelight cache on logout.
 *
 * This is intentionally centralized so we don't forget to wipe a feature
 * database when adding new modules.
 */
class DefaultLocalDatabaseCleaner(
    private val authDatabase: AuthDatabase,
) : LocalDatabaseCleaner {

    private val mutex = Mutex()

    override suspend fun clearAll(): Result<Unit> = runCatching {
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val errors = mutableListOf<Throwable>()

                suspend fun attempt(block: suspend () -> Unit) {
                    runCatching { block() }.onFailure(errors::add)
                }

                attempt { authDatabase.authQueries.clearAllUsers() }

                if (errors.isNotEmpty()) {
                    val primary = errors.first()
                    errors.drop(1).forEach(primary::addSuppressed)
                    throw primary
                }
            }
        }
    }
}
