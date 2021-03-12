package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class InvoiceDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatusAndUpdate(queryStatus: InvoiceStatus, updateStatus: InvoiceStatus):
            List<Pair<Invoice, Customer>> {
        return transaction(db) {
            val invoices = (InvoiceTable innerJoin CustomerTable)
                .select { InvoiceTable.status.eq(queryStatus.name) }
                .map { Pair(it.toInvoice(), it.toCustomer()) }

            // TODO: Update all invoices in one go
            invoices.forEach { invoicePair -> updateInvoiceStatus(invoicePair.first.id, updateStatus) }

            invoices
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
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

        return fetchInvoice(id)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Invoice? {
        transaction(db) {
            InvoiceTable
                .update({ InvoiceTable.id.eq(id) }) {
                    it[InvoiceTable.status] = status.toString()
                }
        }

        return fetchInvoice(id)
    }
}