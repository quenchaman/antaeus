package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.ExchangeProvider
import io.pleo.antaeus.models.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ExchangeService(private val exchangeProvider: ExchangeProvider) {
    fun checkCurrencyMismatch(invoice: Invoice, customer: Customer): Boolean {
        return customer.currency != invoice.amount.currency
    }

    fun exchangeInvoiceAmountToCustomerCurrency(invoice: Invoice, customer: Customer): Invoice {
        logger.info(
            "Sending invoice with id: ${invoice.id} and amount: ${invoice.amount.value} for currency exchange - " +
                    "invoice currency is ${invoice.amount.currency.name} and target currency is ${customer.currency.name}"
        )
        val exchangedAmount = exchangeProvider.exchange(
            invoice.amount.value,
            invoice.amount.currency,
            customer.currency
        )

        logger.info("Invoice successfully exchanged. New amount is: $exchangedAmount")

        return invoice.copy(amount = Money(value = exchangedAmount, currency = customer.currency))
    }
}
