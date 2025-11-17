package ai.dokus.auth.backend.plugins

import ai.dokus.foundation.ktor.database.DatabaseFactory
import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

/**
 * Configures and initializes the database connection.
 * Triggers lazy initialization of the DatabaseFactory.
 */
fun Application.configureDatabase() {
    logger.info("Initializing database connection...")
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        // Database initialization happens in the DatabaseFactory constructor via Koin
        // This line triggers the lazy initialization
        logger.info("Database initialized successfully: ${dbFactory.database}")
    }
}

/**
 * Configures graceful database shutdown on application stop.
 */
fun Application.configureGracefulDatabaseShutdown() {
    monitor.subscribe(io.ktor.server.application.ApplicationStopping) {
        logger.info("Closing database connections...")
        runBlocking {
            val dbFactory by inject<DatabaseFactory>()
            dbFactory.close()
            logger.info("Database connections closed")
        }
    }
}
