package ai.dokus.app.auth.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Use SQLDelight's built-in helper which handles WASM Worker creation correctly
        val driver = createDefaultWebWorkerDriver()

        // Initialize the database schema asynchronously
        // For WASM, we launch in background since we can't block
        MainScope().launch {
            AuthDatabase.Schema.create(driver).await()
        }

        return driver
    }
}