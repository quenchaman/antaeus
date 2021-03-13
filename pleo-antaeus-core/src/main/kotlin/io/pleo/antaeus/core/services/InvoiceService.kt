/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: InvoiceDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchAll()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetch(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchUnpaid(): List<Pair<Invoice, Customer>> {
        return dal.fetchByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT)
    }

    fun changeStatus(id: Int, status: InvoiceStatus): Invoice? {
        return dal.updateStatus(id, status)
    }
}
