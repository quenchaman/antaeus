package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.helpers.InvoiceFactory
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val exchangeService = mockk<ExchangeService> {
        every { checkCurrencyMismatch(any(), any()) } returns true
        every { exchangeInvoiceAmountToCustomerCurrency(any(), any()) } returns InvoiceFactory.create()
    }

    @Test
    fun `will have a method called 'charge'`() {
        val paymentProvider = mockk<PaymentProvider> {}
        val invoiceService = mockk<InvoiceService> {}
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertNotNull(billingService::prepareInvoicesForPayment)
    }

    @Test
    fun `will return empty list when there are no invoices to bill`() {
        val paymentProvider = mockk<PaymentProvider> {}
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns emptyList()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertTrue(billingService.prepareInvoicesForPayment().isEmpty())
    }

    @Test
    fun `will call invoice service method to get unpaid invoices`() {
        val invoice = InvoiceFactory.create()
        val customer = Customer(id = invoice.customerId, currency = invoice.amount.currency)
        val paymentProvider = mockk<PaymentProvider> {}
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns listOf(
                Pair(invoice, customer)
            )
        }
        val exchangeService = mockk<ExchangeService> {
            every { checkCurrencyMismatch(any(), any()) } returns true
            every { exchangeInvoiceAmountToCustomerCurrency(any(), any()) } returns invoice
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        val invoices = billingService.prepareInvoicesForPayment()
        verify(exactly = 1) { invoiceService.fetchUnpaid() }
        Assertions.assertTrue(invoices.isNotEmpty())
        Assertions.assertTrue(invoice == invoices.first())
    }

    @Test
    fun `will call exchange service to filter mismatching currencies`() {
        val money = Money(BigDecimal.TEN, Currency.DKK)
        val invoice = InvoiceFactory.create(amount = money)
        val customer = Customer(id = invoice.customerId, currency = Currency.USD)
        val paymentProvider = mockk<PaymentProvider> {}
        val invoices = listOf(
            Pair(invoice, customer),
            Pair(InvoiceFactory.create(), customer)
        )
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns invoices
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.prepareInvoicesForPayment()
        verify(exactly = invoices.size) { exchangeService.checkCurrencyMismatch(any(), any()) }
    }

    @Test
    fun `will call method to exchange invoice amount to customer currency`() {
        val money = Money(BigDecimal.TEN, Currency.DKK)
        val invoice = InvoiceFactory.create(amount = money)
        val customer = Customer(id = invoice.customerId, currency = Currency.USD)
        val paymentProvider = mockk<PaymentProvider> {}
        val invoices = listOf(
            Pair(invoice, customer),
            Pair(InvoiceFactory.create(), customer)
        )
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns invoices
        }
        val exchangeService = mockk<ExchangeService> {
            every { exchangeInvoiceAmountToCustomerCurrency(any(), any()) } returns invoice
            every { checkCurrencyMismatch(any(), any()) } returns true
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.prepareInvoicesForPayment()
        verify(exactly = 1) { exchangeService.exchangeInvoiceAmountToCustomerCurrency(invoice, customer) }
    }

    @Test
    fun `will call method to exchange invoices the correct number of times`() {
        val customer = Customer(id = 1, currency = Currency.USD)
        val mismatchingInvoice1 = InvoiceFactory.create(customerId = customer.id,
            amount = Money(value = BigDecimal.ONE, currency = Currency.EUR))

        val mismatchingInvoice2 = InvoiceFactory.create(customerId = customer.id,
            amount = Money(value = BigDecimal.ONE, currency = Currency.DKK))

        val matchingInvoice = InvoiceFactory.create(customerId = customer.id,
            amount = Money(value = BigDecimal.ONE, currency = customer.currency))

        val allInvoices = listOf(mismatchingInvoice1, mismatchingInvoice2, matchingInvoice)
            .map { Pair(it, customer) }

        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns allInvoices
        }
        val exchangeService = mockk<ExchangeService> {
            every { exchangeInvoiceAmountToCustomerCurrency(any(), any()) } returns InvoiceFactory.create()
            every { checkCurrencyMismatch(mismatchingInvoice1, any()) } returns true
            every { checkCurrencyMismatch(mismatchingInvoice2, any()) } returns true
            every { checkCurrencyMismatch(matchingInvoice, any()) } returns false
        }
        val paymentProvider = mockk<PaymentProvider> {}

        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)
        billingService.prepareInvoicesForPayment()
        verify(exactly = 2) { exchangeService.exchangeInvoiceAmountToCustomerCurrency(any(), any()) }
    }
}