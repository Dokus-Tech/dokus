package tech.dokus.features.cashflow.cache

import tech.dokus.domain.model.common.Feature
import tech.dokus.foundation.app.database.DatabaseWrapper

/**
 * Database wrapper for the Cashflow cache database.
 * Handles thread-safe initialization and provides access to the CashflowCacheDatabase.
 */
class CashflowDb private constructor() : DatabaseWrapper<CashflowCacheDatabase> by DatabaseWrapper(
    feature = Feature.Cashflow,
    schema = CashflowCacheDatabase.Schema,
    createDatabase = { driver -> CashflowCacheDatabase(driver) }
) {
    companion object {
        fun create(): CashflowDb = CashflowDb()
    }
}
