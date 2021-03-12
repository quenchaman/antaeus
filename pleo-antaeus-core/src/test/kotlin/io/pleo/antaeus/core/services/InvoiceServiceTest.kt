package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.services.helpers.InvoiceFactory
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InvoiceServiceTest {

    private val defaultCustomer = Customer(id = 1, currency = Currency.GBP)

    private val unpaidInvoices: List<Pair<Invoice, Customer>> = listOf(
        InvoiceFactory.create(),
        InvoiceFactory.create(),
        InvoiceFactory.create(),
        InvoiceFactory.create(),
        InvoiceFactory.create()
    ).map { Pair(it, defaultCustomer) }

    @Test
    fun `will throw if invoice is not found`() {
        val dal = mockk<AntaeusDal> {
            every { fetchInvoice(404) } returns null
        }

        val invoiceService = InvoiceService(dal = dal)

        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(404)
        }
    }

    @Test
    fun `will return unpaid invoices with customer`() {
        val dal = mockk<AntaeusDal> {
            every {
                fetchInvoicesByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT)
            } returns unpaidInvoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val invoices: List<Pair<Invoice, Customer>> = invoiceService.fetchUnpaid()

        verify(exactly = 1) { dal.fetchInvoicesByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT) }
        Assertions.assertTrue(invoices.isNotEmpty())
        Assertions.assertEquals(unpaidInvoices.size, invoices.size)

        val someInvoice: Pair<Invoice, Customer> = invoices.first()
        Assertions.assertEquals(InvoiceStatus.PENDING, someInvoice.first.status)
        Assertions.assertEquals(someInvoice.first.customerId, someInvoice.second.id)
    }

    @Test
    fun `will return only unpaid invoices`() {
        val dal = mockk<AntaeusDal> {
            every {
                fetchInvoicesByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT)
            } returns unpaidInvoices
        }
        val invoiceService = InvoiceService(dal = dal)
        val invoices: List<Pair<Invoice, Customer>> = invoiceService.fetchUnpaid()

        invoices.forEach { Assertions.assertEquals(InvoiceStatus.PENDING, it.first.status) }
    }

    @Test
    fun `will return an empty list when there are no invoices in the db`() {
        val dal = mockk<AntaeusDal> {
            every {
                fetchInvoicesByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT)
            } returns emptyList()
        }

        val invoiceService = InvoiceService(dal = dal)

        Assertions.assertTrue(invoiceService.fetchUnpaid().isEmpty())
    }

    @Test
    fun `will join invoice properly with customer`() {
        val customer1 = Customer(id = 1, currency = Currency.DKK)
        val customer2 = Customer(id = 2, currency = Currency.SEK)
        val customer3 = Customer(id = 3, currency = Currency.USD)
        val invoice1: Invoice = InvoiceFactory.create(customerId = customer1.id)
        val invoice2: Invoice = InvoiceFactory.create(customerId = customer2.id)
        val invoice3: Invoice = InvoiceFactory.create(customerId = customer3.id)
        val unpaidInvoices = listOf(
            Pair(invoice1, customer1),
            Pair(invoice2, customer2),
            Pair(invoice3, customer3)
        )

        val dal = mockk<AntaeusDal> {
            every { fetchInvoicesByStatusAndUpdate(InvoiceStatus.PENDING, InvoiceStatus.SENT_FOR_PAYMENT) } returns unpaidInvoices
        }

        val invoiceService = InvoiceService(dal = dal)

        val actualInvoices: List<Pair<Invoice, Customer?>> = invoiceService.fetchUnpaid()

        Assertions.assertEquals(customer1.id, actualInvoices[0].second?.id)
        Assertions.assertEquals(customer2.id, actualInvoices[1].second?.id)
        Assertions.assertEquals(customer3.id, actualInvoices[2].second?.id)
    }

    @Test
    fun `will have method to update invoice status and it calls dal`() {
        val dal = mockk<AntaeusDal> {
            every { updateInvoiceStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val invoiceService = InvoiceService(dal = dal)

        invoiceService.changeStatus(1, InvoiceStatus.PAID)
        verify(exactly = 1) { dal.updateInvoiceStatus(any(), any()) }
    }
}
