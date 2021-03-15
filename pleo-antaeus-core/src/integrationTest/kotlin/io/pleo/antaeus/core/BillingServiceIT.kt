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
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.*
import java.math.BigDecimal

class BillingServiceIT {

    private val tables = arrayOf(InvoiceTable as Table, CustomerTable as Table)
    private val testDb = SqLiteDBConnection.connect("antaeus-test-db", tables)
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
        invoiceDal.delete()
        customerDal.delete()

        customerDal.create(currency = customer1.currency)

        (1..10).forEach { number -> invoiceDal.create(
            amount = Money(value = BigDecimal(number), currency = customer1.currency),
            customer = customer1
        )}
    }

    @AfterEach
    fun cleanUp() {
        invoiceDal.delete()
        customerDal.delete()
    }

    @Test
    fun `will charge all unpaid invoices`() {
        val unpaidInvoices = invoiceDal.fetchAll()
            .filter { it.status == InvoiceStatus.PENDING }
        val unpaidInvoicesCount = unpaidInvoices.size

        billingService.charge()
        verify(exactly = unpaidInvoicesCount) { billingService.charge(any()) }
        verify(exactly = unpaidInvoicesCount) { paymentProvider.charge(any()) }
        val allInvoices = invoiceDal.fetchAll()

        Assertions.assertEquals(
            allInvoices.size,
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }

    @Test
    fun `will exchange and charge non-matching invoices`() {
        invoiceDal.create(amount = unpaidMismatchInvoice1.amount, customer = customer1, status = InvoiceStatus.PENDING)
        invoiceDal.create(amount = unpaidMismatchInvoice2.amount, customer = customer1, status = InvoiceStatus.PENDING)

        val unpaidInvoices = invoiceDal.fetchAll()
            .filter { it.status == InvoiceStatus.PENDING }
        val unpaidInvoicesCount = unpaidInvoices.size

        billingService.charge()
        verify(exactly = unpaidInvoicesCount) { billingService.charge(any()) }
        verify(exactly = 2) { exchangeService.exchange(any(), any(), any()) }
        val allInvoices = invoiceDal.fetchAll()

        Assertions.assertEquals(
            allInvoices.size,
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }

    @Test
    fun `will handle missing customer exception and charge all other invoices`() {
        val customer2 = customerDal.create(Currency.EUR)
        val invoice = invoiceDal.create(
            amount = unpaidMismatchInvoice1.amount,
            customer = customer2 ?: customer1,
            status = InvoiceStatus.PENDING
        ) ?: unpaidMismatchInvoice1

        billingService.charge()

        Assertions.assertEquals(InvoiceStatus.CUSTOMER_NOT_FOUND, invoiceDal.fetch(invoice.id)?.status)
        val allInvoices = invoiceDal.fetchAll()
        Assertions.assertEquals(allInvoices.size - 1, // without the customer not found one
            allInvoices.filter { it.status == InvoiceStatus.PAID }.size
        )
    }
}
