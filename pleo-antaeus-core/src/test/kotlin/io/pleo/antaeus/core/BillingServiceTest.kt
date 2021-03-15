package io.pleo.antaeus.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.ExchangeService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.InvoiceFactory
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val exchangeService = mockk<ExchangeService> {
        every { checkCurrencyMismatch(any(), any()) } returns true
        every { exchangeInvoiceAmountToCustomerCurrency(any(), any()) } returns InvoiceFactory.create()
    }

    private val paymentProvider = mockk<PaymentProvider> {}

    @Test
    fun `will have a method called 'charge'`() {
        val invoiceService = mockk<InvoiceService> {}
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertNotNull(billingService::prepareInvoicesForPayment)
    }

    @Test
    fun `will return empty list when there are no invoices to bill`() {
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

        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)
        billingService.prepareInvoicesForPayment()
        verify(exactly = 2) { exchangeService.exchangeInvoiceAmountToCustomerCurrency(any(), any()) }
    }

    @Test
    fun `will have a method for charging prepared invoices`() {
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns emptyList()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge()
        Assertions.assertTrue(true)
    }

    @Test
    fun `will call payment provider method to charge invoice for each not paid invoice`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val unpaidInvoices = listOf(
            Pair(InvoiceFactory.create(), Customer(id = 1, currency = Currency.EUR)),
            Pair(InvoiceFactory.create(), Customer(id = 1, currency = Currency.EUR))
        )
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns unpaidInvoices
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge()
        verify(exactly = unpaidInvoices.size) { paymentProvider.charge(any()) }
    }

    // charge(Invoice)

    @Test
    fun `will have a method to charge a single invoice`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge(InvoiceFactory.create())
        Assertions.assertTrue(true)
    }

    @Test
    fun `will call the method to charge invoice in payment provider`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge(InvoiceFactory.create())
        verify(exactly = 1) { paymentProvider.charge(any()) }
    }

    @Test
    fun `will return do not retry=false when payment provider call fails with network error`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws NetworkException()
        }
        val invoiceService = mockk<InvoiceService> {}
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertFalse(billingService.charge(InvoiceFactory.create()))
    }

    @Test
    fun `will return do not retry=true when payment provider call fails with customer missing error`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws CustomerNotFoundException(1)
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertTrue(billingService.charge(InvoiceFactory.create()))
    }

    @Test
    fun `will return do not retry=true when payment provider call fails with currency mismatch error`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws CurrencyMismatchException(1, 1)
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertTrue(billingService.charge(InvoiceFactory.create()))
    }

    @Test
    fun `will return do not retry=true when payment provider returns false`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns false
        }
        val invoiceService = mockk<InvoiceService> {}
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertTrue(billingService.charge(InvoiceFactory.create()))
    }

    @Test
    fun `will return do not retry=true when payment provider charge is successful`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        Assertions.assertTrue(billingService.charge(InvoiceFactory.create()))
    }

    @Test
    fun `will call method to update invoice status when successful`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } returns true
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge(InvoiceFactory.create())

        verify(exactly = 1) { invoiceService.changeStatus(any(), InvoiceStatus.PAID) }
    }

    @Test
    fun `will set invoice status for review when customer is not found`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws CustomerNotFoundException(1)
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge(InvoiceFactory.create())

        verify(exactly = 1) { invoiceService.changeStatus(any(), InvoiceStatus.CUSTOMER_NOT_FOUND) }
    }

    @Test
    fun `will set invoice status for review when currency mismatch`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws CurrencyMismatchException(1, 1)
        }
        val invoiceService = mockk<InvoiceService> {
            every { changeStatus(any(), any()) } returns InvoiceFactory.create()
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge(InvoiceFactory.create())

        verify(exactly = 1) { invoiceService.changeStatus(any(), InvoiceStatus.CURRENCY_MISMATCH) }
    }

    @Test
    fun `will retry on network exception`() {
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(any()) } throws NetworkException()
        }
        val unpaidInvoices = listOf(
            Pair(InvoiceFactory.create(), Customer(id = 1, currency = Currency.EUR)),
            Pair(InvoiceFactory.create(), Customer(id = 1, currency = Currency.EUR))
        )
        val invoiceService = mockk<InvoiceService> {
            every { fetchUnpaid() } returns unpaidInvoices
        }
        val billingService = BillingService(paymentProvider, invoiceService, exchangeService)

        billingService.charge()
        verify(exactly = 3 * unpaidInvoices.size) { paymentProvider.charge(any()) }
    }
}
