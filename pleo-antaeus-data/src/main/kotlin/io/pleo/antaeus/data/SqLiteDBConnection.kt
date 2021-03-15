package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

class SqLiteDBConnection {
    companion object {
        @JvmStatic
        fun connect(dbName: String, tables: Array<out Table>): Database {
            val dbFile: File = File.createTempFile(dbName, ".sqlite")
            return Database
                .connect(url = "jdbc:sqlite:${dbFile.absolutePath}",
                    driver = "org.sqlite.JDBC",
                    user = "root",
                    password = "")
                .also {
                    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                    transaction(it) {
                        addLogger(StdOutSqlLogger)
                        // Drop all existing tables to ensure a clean slate on each run
                        SchemaUtils.drop(*tables)
                        // Create all tables
                        SchemaUtils.create(*tables)
                    }
                }
        }
    }
}
