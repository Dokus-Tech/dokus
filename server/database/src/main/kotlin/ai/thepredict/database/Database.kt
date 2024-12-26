package ai.thepredict.database

import ai.thepredict.configuration.ServerEndpoint
import org.jetbrains.exposed.sql.Database

val db by lazy {
    Database.connect(
//        "jdbc:postgresql://${ServerEndpoint.Database.connectUrl}/postgres",
        "jdbc:pgsql://${ServerEndpoint.Database.connectUrl}/postgres",
        driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = "postgres",
        password = "predictme"
    )
}