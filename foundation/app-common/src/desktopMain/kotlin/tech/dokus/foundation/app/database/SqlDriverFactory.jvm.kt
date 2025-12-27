package tech.dokus.foundation.app.database

import tech.dokus.domain.model.common.Feature
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    return withContext(Dispatchers.IO) {
        val databasePath = File(System.getProperty("user.home"), ".dokus/${frontendDbName}.db")
        databasePath.parentFile?.mkdirs()

        val driver = JdbcSqliteDriver(url = "jdbc:sqlite:${databasePath.absolutePath}")
        schema.create(driver).await()
        driver
    }
}

actual suspend fun Feature.deleteSqlDatabase() {
    withContext(Dispatchers.IO) {
        val databasePath = File(System.getProperty("user.home"), ".dokus/${frontendDbName}.db")
        databasePath.delete()
    }
}
