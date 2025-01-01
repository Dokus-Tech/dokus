package ai.thepredict.database

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.database.tables.ContactNotesTable
import ai.thepredict.database.tables.ContactsTable
import ai.thepredict.database.tables.UsersTable
import ai.thepredict.database.tables.WorkspacesTable
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    private val serverEndpoint: ServerEndpoint.Database = ServerEndpoint.Database

    private val tables: Array<Table> = arrayOf(
        UsersTable,
        WorkspacesTable,
        ContactsTable,
        ContactNotesTable
    )

    private val db by lazy {
        Database.connect(
//        "jdbc:postgresql://${ServerEndpoint.Database.connectUrl}/postgres",
            "jdbc:pgsql://${serverEndpoint.connectUrl}/postgres",
            driver = "com.impossibl.postgres.jdbc.PGDriver",
            user = "postgres",
            password = "predictme"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    suspend fun <T> transaction(statement: suspend Transaction.() -> T): T {
        val databaseContext = newSingleThreadContext("DatabaseThread")
        return suspendedTransactionAsync(databaseContext, db) {
            SchemaUtils.create(*tables)
            addLogger(StdOutSqlLogger)
            statement()
        }.await()
    }
}