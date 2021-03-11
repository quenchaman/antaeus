package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.ExchangeProvider
import io.pleo.antaeus.models.*

class ExchangeService(private val exchangeProvider: ExchangeProvider) {
    fun checkCurrencyMismatch(invoice: Invoice, customer: Customer): Boolean {
        return customer.currency != invoice.amount.currency
    }

    fun exchangeInvoiceAmountToCustomerCurrency(invoice: Invoice, customer: Customer): Invoice {
        val exchangedAmount = exchangeProvider.exchange(
            invoice.amount.value,
            invoice.amount.currency,
            customer.currency
        )

        return invoice.copy(amount = Money(value = exchangedAmount, currency = customer.currency))
    }
}
