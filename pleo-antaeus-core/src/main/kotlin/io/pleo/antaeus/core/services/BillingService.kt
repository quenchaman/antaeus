package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val exchangeService: ExchangeService
) {
    fun prepareInvoicesForPayment(): List<Invoice> {
        val (mismatched, notMismatched) = invoiceService.fetchUnpaid()
            .partition { exchangeService.checkCurrencyMismatch(it.first, it.second) }

        return notMismatched.map { it.first } +
                mismatched.map { exchangeService.exchangeInvoiceAmountToCustomerCurrency(it.first, it.second) }

    }
}
