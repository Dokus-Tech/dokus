package tech.dokus.foundation.app.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import tech.dokus.domain.model.common.Feature

@Suppress("EXPECT_ACTUAL_INCOMPATIBILITY_DEPRECATION")
expect suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver

expect suspend fun Feature.deleteSqlDatabase()
