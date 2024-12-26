package ai.thepredict.database

import ai.thepredict.configuration.ServerEndpoint
import org.jetbrains.exposed.sql.Database

val db by lazy {
    Database.connect(
        "jdbc:postgresql://${ServerEndpoint.Database.connectUrl}",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "predictme"
    )
}