package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class InvoiceDal(private val db: Database) : BaseDal() {

    fun fetch(id: Int): Invoice? = transaction(db) {
        fetchById(InvoiceTable, id)?.toInvoice()
    }

    fun fetchAll(): List<Invoice> = transaction(db) {
        fetchAll(InvoiceTable).map { it.toInvoice() }
    }

    fun fetchByStatusAndUpdate(queryStatus: InvoiceStatus, updateStatus: InvoiceStatus): List<Pair<Invoice, Customer>> {
        return transaction(db) {
            val invoices = (InvoiceTable innerJoin CustomerTable)
                .select { InvoiceTable.status.eq(queryStatus.name) }
                .map { Pair(it.toInvoice(), it.toCustomer()) }

            // TODO: Update all invoices in one go
            invoices.forEach { invoicePair -> updateStatus(invoicePair.first.id, updateStatus) }

            invoices
        }
    }

    fun create(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetch(id)
    }

    fun updateStatus(id: Int, status: InvoiceStatus): Invoice? {
        transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id.eq(id) }) {
                    it[InvoiceTable.status] = status.toString()
                }
        }

        return fetch(id)
    }

    fun delete() = transaction(db) {
        deleteAll(InvoiceTable)
    }
}
