package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class CustomerDal(private val db: Database) : BaseDal() {

    fun fetch(id: Int): Customer? = transaction(db) {
        fetchById(CustomerTable, id)?.toCustomer()
    }

    fun fetchAll(): List<Customer> = transaction {
        fetchAll(CustomerTable).map { it.toCustomer() }
    }

    fun create(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetch(id)
    }

    fun delete() = transaction(db) {
        deleteAll(CustomerTable)
    }
}
