package tech.dokus.foundation.app.database

import ai.dokus.foundation.domain.model.common.Feature
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

@Suppress("EXPECT_ACTUAL_INCOMPATIBILITY_DEPRECATION")
expect suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver

expect suspend fun Feature.deleteSqlDatabase()
