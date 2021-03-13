package io.pleo.antaeus.core.services

import kotlinx.coroutines.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.retryIO
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val exchangeService: ExchangeService
) {
    fun prepareInvoicesForPayment(): List<Invoice> {
        logger.info("Preparing invoices for payment...")

        val (mismatched, matching) = invoiceService.fetchUnpaid()
            .partition { exchangeService.checkCurrencyMismatch(it.first, it.second) }

        logger.info("Fetched ${mismatched.size} currency mismatching invoices and ${matching.size} matching invoices.")

        return matching.map { it.first } +
                mismatched.map { exchangeService.exchangeInvoiceAmountToCustomerCurrency(it.first, it.second) }
    }

    fun charge() = runBlocking {
        prepareInvoicesForPayment()
            .map {
                async(Dispatchers.IO) {
                    retryIO(times = 3) { charge(it) }
                }
            }
            .map { it.await() }
            .forEach { _ -> }
    }

    /*
        Returns:
          `True` when no retry is needed
          `False` when the call should be retried
     */
    fun charge(invoice: Invoice): Boolean {
        logger.info("Sending invoice ${invoice.id} for payment...")

        try {
            val isSuccess = paymentProvider.charge(invoice)

            if (!isSuccess) {
                logger.warn("Invoice ${invoice.id} was not processed successfully by payment provider.")
                return true
            }

            logger.info("Invoice ${invoice.id} was paid")
            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.PAID)
        } catch (err: NetworkException) {
            logger.error("Payment for invoice ${invoice.id} resulted in network error - retrying payment")
            return false
        } catch (err: CustomerNotFoundException) {
            logger.warn("Customer was not found in payment provider for invoice ${invoice.id}")
            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.CUSTOMER_NOT_FOUND)
            return true
        } catch (err: CurrencyMismatchException) {
            logger.warn("Invoice ${invoice.id} currency does not match customer currency")
            invoiceService.changeStatus(id = invoice.id, status = InvoiceStatus.CURRENCY_MISMATCH)
            return true
        }

        return true
    }
}
