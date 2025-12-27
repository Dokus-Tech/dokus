package tech.dokus.backend.plugins

import tech.dokus.foundation.ktor.database.DatabaseFactory
import tech.dokus.foundation.ktor.utils.loggerFor
import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.inject

private val logger = loggerFor("Database")

fun Application.configureDatabase() {
    logger.info("Initializing database connection...")
    runBlocking {
        val dbFactory by inject<DatabaseFactory>()
        logger.info("Database initialized successfully: ${dbFactory.database}")
    }
}

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
