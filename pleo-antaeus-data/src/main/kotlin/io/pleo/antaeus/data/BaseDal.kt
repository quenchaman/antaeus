package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.*

open class BaseDal() {

    fun <T> fetchById(table: T, id: Int): ResultRow? where T: Table, T: Identity<Int> {
        return table
            .select { table.id.eq(id) }
            .firstOrNull()
    }

    fun <T> fetchAll(table: T): Query where T: Table, T: Identity<Int> {
        return table.selectAll()
    }

    fun <T> deleteAll(table: T) where T: Table, T: Identity<Int> {
        table.deleteAll()
    }

}
