package io.pleo.antaeus.core.services.unit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.ExchangeProvider
import io.pleo.antaeus.core.services.ExchangeService
import io.pleo.antaeus.core.services.helpers.InvoiceFactory
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeServiceTest {

    @Test
    fun `will have a method to check for mismatch in currency`() {
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns BigDecimal.TEN
        }
        val exchangeService = ExchangeService(exchangeProvider = exchangeProvider)
        val customer = Customer(id = 1, currency = Currency.DKK)

        Assertions.assertNotNull(exchangeService::checkCurrencyMismatch)
        exchangeService.checkCurrencyMismatch(InvoiceFactory.create(), customer)
    }

    @Test
    fun `will return false if customer currency matches invoice currency`() {
        val customerId = 1
        val customerCurrency = Currency.USD
        val customer = Customer(id = customerId, currency = customerCurrency)
        val invoice = InvoiceFactory.create(customerId = customerId, amount = Money(BigDecimal.TEN, customerCurrency))
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns BigDecimal.TEN
        }
        val exchangeService = ExchangeService(exchangeProvider)

        Assertions.assertFalse(exchangeService.checkCurrencyMismatch(invoice, customer))
    }

    @Test
    fun `will return true if customer currency does not match invoice currency`() {
        val customerId = 1
        val customerCurrency = Currency.USD
        val customer = Customer(id = customerId, currency = customerCurrency)
        val invoice = InvoiceFactory.create(customerId = customerId, amount = Money(BigDecimal.TEN, Currency.DKK))
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns BigDecimal.TEN
        }
        val exchangeService = ExchangeService(exchangeProvider)

        Assertions.assertTrue(exchangeService.checkCurrencyMismatch(invoice, customer))
    }

    @Test
    fun `will have a method to exchange invoice amount to customer currency`() {
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns BigDecimal.TEN
        }
        val exchangeService = ExchangeService(exchangeProvider)

        Assertions.assertNotNull(exchangeService::exchangeInvoiceAmountToCustomerCurrency)
        exchangeService.exchangeInvoiceAmountToCustomerCurrency(
            InvoiceFactory.create(),
            Customer(id = 1, currency = Currency.SEK)
        )
    }

    @Test
    fun `will call ExchangeProvider charge method with correct params`() {
        val customerId = 1
        val customer = Customer(id = customerId, currency = Currency.DKK)
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns BigDecimal.TEN
        }
        val exchangeService = ExchangeService(exchangeProvider)
        val invoice = InvoiceFactory.create()

        exchangeService.exchangeInvoiceAmountToCustomerCurrency(invoice, customer)

        verify(exactly = 1) { exchangeProvider.exchange(
            amount = invoice.amount.value,
            currency = invoice.amount.currency,
            targetCurrency = customer.currency
        ) }
    }

    @Test
    fun `will return invoice with amount returned from exchange provider and customer currency`() {
        val customerId = 1
        val customer = Customer(id = customerId, currency = Currency.DKK)
        val exchangedAmount = BigDecimal.valueOf(1000)
        val exchangeProvider = mockk<ExchangeProvider> {
            every { exchange(any(), any(), any()) } returns exchangedAmount
        }
        val exchangeService = ExchangeService(exchangeProvider)
        val invoice = InvoiceFactory.create()
        val exchangedInvoice: Invoice = exchangeService.exchangeInvoiceAmountToCustomerCurrency(invoice, customer)

        Assertions.assertEquals(exchangedAmount, exchangedInvoice.amount.value)
        Assertions.assertEquals(customer.currency, exchangedInvoice.amount.currency)
    }
}