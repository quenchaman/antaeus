package io.pleo.antaeus.core

import io.mockk.spyk
import io.mockk.verify
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.ExchangeService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.external.getExchangeProvider
import io.pleo.antaeus.core.utils.InvoiceFactory
import io.pleo.antaeus.core.utils.getTestPaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.*
import java.math.BigDecimal

class BillingServiceIT {

    private val tables = arrayOf(InvoiceTable, CustomerTable)
    private val testDb = DBConnection.connect("antaeus-test-db", tables)
    private val invoiceDal = InvoiceDal(testDb)
    private val customerDal = CustomerDal(testDb)
    private val invoiceService = InvoiceService(invoiceDal)
    private val exchangeService = spyk(getExchangeProvider())
    private val paymentProvider = spyk(getTestPaymentProvider())
    private val billingService = spyk(BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        exchangeService = ExchangeService(exchangeService)
    ))

    private val customer1 = Customer(id = 1, currency = Currency.EUR)
    private val unpaidMismatchInvoice1 = InvoiceFactory.create(customerId = 1,
        amount = Money(value = BigDecimal.ONE, currency = Currency.DKK))
    private val unpaidMismatchInvoice2 = InvoiceFactory.create(customerId = 1,
        amount = Money(value = BigDecimal.ONE, currency = Currency.USD))

    @BeforeEach
    fun init() {
        invoiceDal.clearInvoices()
        customerDal.clearCustomers()

        customerDal.createCustomer(currency = customer1.currency)

        (1..10).forEach { number -> invoiceDal.createInvoice(
            amount = Money(value = BigDecimal(number), currency = customer1.currency),
            customer = customer1
        )}
    }

    @AfterEach
    fun cleanUp() {
        invoiceDal.clearInvoices()
        customerDal.clearCustomers()
    }

    @Test
    fun `will charge all unpaid invoices`() {
        val unpaidInvoices = invoiceDal.fetchInvoices()
            .filter { it.status == InvoiceStatus.PENDING }
        val unpaidInvoicesCount = unpaidInvoices.size

        billingService.charge()
        verify(exactly = unpaidInvoicesCount) { billingService.charge(any()) }
        verify(exactly = unpaidInvoicesCount) { paymentProvider.charge(any()) }
        val allInvoices = invoiceDal.fetchInvoices()

        Assertions.assertEquals(
            allInvoices.size,
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }

    @Test
    fun `will exchange and charge non-matching invoices`() {
        invoiceDal.createInvoice(amount = unpaidMismatchInvoice1.amount, customer = customer1, status = InvoiceStatus.PENDING)
        invoiceDal.createInvoice(amount = unpaidMismatchInvoice2.amount, customer = customer1, status = InvoiceStatus.PENDING)

        val unpaidInvoices = invoiceDal.fetchInvoices()
            .filter { it.status == InvoiceStatus.PENDING }
        val unpaidInvoicesCount = unpaidInvoices.size

        billingService.charge()
        verify(exactly = unpaidInvoicesCount) { billingService.charge(any()) }
        verify(exactly = 2) { exchangeService.exchange(any(), any(), any()) }
        val allInvoices = invoiceDal.fetchInvoices()

        Assertions.assertEquals(
            allInvoices.size,
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }

    @Test
    fun `will handle missing customer exception and charge all other invoices`() {
        val customer2 = customerDal.createCustomer(Currency.EUR)
        val invoice = invoiceDal.createInvoice(
            amount = unpaidMismatchInvoice1.amount,
            customer = customer2 ?: customer1,
            status = InvoiceStatus.PENDING
        ) ?: unpaidMismatchInvoice1

        billingService.charge()

        Assertions.assertEquals(InvoiceStatus.CUSTOMER_NOT_FOUND, invoiceDal.fetchInvoice(invoice.id)?.status)
        val allInvoices = invoiceDal.fetchInvoices()
        Assertions.assertEquals(allInvoices.size - 1, // without the customer not found one
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }
}
