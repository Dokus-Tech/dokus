package ai.dokus.processor.backend.plugins

import ai.dokus.foundation.ktor.config.AppBaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ProcessorDatabase")
private var dataSource: HikariDataSource? = null

fun Application.configureDatabase() {
    val appConfig by inject<AppBaseConfig>()

    logger.info("Connecting to database: ${appConfig.database.url}")

    val hikariConfig = HikariConfig().apply {
        driverClassName = appConfig.database.driver
        jdbcUrl = appConfig.database.url
        username = appConfig.database.username
        password = appConfig.database.password
        maximumPoolSize = appConfig.database.pool.maxSize
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource!!)

    logger.info("Database connected successfully")
}

fun Application.configureGracefulDatabaseShutdown() {
    monitor.subscribe(ApplicationStopping) {
        logger.info("Closing database connection pool...")
        dataSource?.close()
        logger.info("Database connection pool closed")
    }
}
