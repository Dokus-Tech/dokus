package ai.dokus.app.auth.database

import app.cash.sqldelight.db.SqlDriver

class Database private constructor(
    driver: SqlDriver
) : AuthDatabase by AuthDatabase.Companion(driver) {

    companion object {
        fun create(driverFactory: DatabaseDriverFactory): Database {
            val driver = driverFactory.createDriver()
            return Database(driver)
        }
    }
}