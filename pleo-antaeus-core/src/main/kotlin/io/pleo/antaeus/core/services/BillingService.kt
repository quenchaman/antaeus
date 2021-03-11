package io.pleo.antaeus.core.services

import kotlinx.coroutines.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

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

    fun charge() = runBlocking {
        prepareInvoicesForPayment()
            .map { invoice ->
                async(Dispatchers.IO) {
                    charge(invoice)
                }
            }
            .mapNotNull { invoiceCall -> invoiceCall.await() }
            .forEach { invoice -> println(invoice) }
    }

    /*
        Returns:
          `True` when no retry is needed
          `False` when the call should be retried
     */
    fun charge(invoice: Invoice): Boolean {
        try {
            val isSuccess = paymentProvider.charge(invoice)

            if (!isSuccess) {
                return true
            }

            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.PAID)
        } catch (err: NetworkException) {
            return false
        } catch (err: CustomerNotFoundException) {
            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.CUSTOMER_NOT_FOUND)
            return true
        } catch (err: CurrencyMismatchException) {
            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.CURRENCY_MISMATCH)
            return true
        }

        return true
    }
}
