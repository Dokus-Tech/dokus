package ai.dokus.app.auth.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Use file-based database for JVM/Desktop to persist data during development
        val databasePath = File(System.getProperty("user.home"), ".dokus/dokus_auth.db")
        databasePath.parentFile?.mkdirs()

        println("Creating Auth SQLite database at: ${databasePath.absolutePath}")

        // Create driver with synchronous schema
        val driver: SqlDriver = JdbcSqliteDriver(
            url = "jdbc:sqlite:${databasePath.absolutePath}",
            schema = AuthDatabase.Schema.synchronous()
        )

        println("Auth database initialized at: ${databasePath.absolutePath}")
        return driver
    }
}