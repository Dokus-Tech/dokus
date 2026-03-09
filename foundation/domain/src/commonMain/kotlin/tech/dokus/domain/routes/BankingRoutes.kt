package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus

/**
 * Type-safe route definitions for Banking API.
 * Base path: /api/v1/banking
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/banking")
class Banking {
    /**
     * GET /api/v1/banking/accounts - List bank connections
     */
    @Serializable
    @Resource("accounts")
    class Accounts(val parent: Banking = Banking())

    /**
     * GET /api/v1/banking/accounts/summary - Aggregate balance summary
     */
    @Serializable
    @Resource("accounts/summary")
    class AccountsSummary(val parent: Banking = Banking())

    /**
     * GET /api/v1/banking/accounts/balance-history - Daily balance per account over a period
     *
     * @param days Number of days to look back (default 30)
     */
    @Serializable
    @Resource("accounts/balance-history")
    class AccountsBalanceHistory(val parent: Banking = Banking(), val days: Int = 30)

    /**
     * GET /api/v1/banking/transactions - List transactions with filters
     *
     * @param status Filter by transaction status
     * @param source Filter by transaction source (BankImport, LiveSync, Manual)
     * @param fromDate Start of date range
     * @param toDate End of date range
     * @param limit Page size (default 50, max 200)
     * @param offset Pagination offset
     */
    @Serializable
    @Resource("transactions")
    class Transactions(
        val parent: Banking = Banking(),
        val status: BankTransactionStatus? = null,
        val source: BankTransactionSource? = null,
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null,
        val limit: Int = 50,
        val offset: Int = 0
    ) {
        /**
         * GET /api/v1/banking/transactions/summary - Transaction counts per status + unresolved total
         */
        @Serializable
        @Resource("summary")
        class Summary(val parent: Transactions = Transactions())

        /**
         * GET /api/v1/banking/transactions/{id} - Get single transaction
         */
        @Serializable
        @Resource("{id}")
        class Id(val parent: Transactions = Transactions(), val id: String) {
            /**
             * POST /api/v1/banking/transactions/{id}/link - Link to cashflow entry
             */
            @Serializable
            @Resource("link")
            class Link(val parent: Id)

            /**
             * POST /api/v1/banking/transactions/{id}/ignore - Mark as ignored
             */
            @Serializable
            @Resource("ignore")
            class Ignore(val parent: Id)

            /**
             * POST /api/v1/banking/transactions/{id}/confirm - Confirm suggested match
             */
            @Serializable
            @Resource("confirm")
            class Confirm(val parent: Id)
        }
    }
}
