package ai.dokus.media.backend.plugins

import ai.dokus.foundation.ktor.database.DatabaseFactory
import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

fun Application.configureDatabase() {
    logger.info("Initializing media database connection...")
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        logger.info("Database initialized: ${dbFactory.database}")
    }
}

fun Application.configureGracefulDatabaseShutdown() {
    monitor.subscribe(io.ktor.server.application.ApplicationStopping) {
        logger.info("Closing media database connections...")
        runBlocking {
            val dbFactory by inject<DatabaseFactory>()
            dbFactory.close()
            logger.info("Database connections closed")
        }
    }
}
