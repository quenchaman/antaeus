package io.pleo.antaeus.core.services.helpers

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal

class InvoiceFactory {

    companion object {
        fun create(
            status: InvoiceStatus = InvoiceStatus.PENDING,
            amount: Money = Money(BigDecimal.TEN, currency = Currency.EUR),
            customerId: Int = 1): Invoice {
            return Invoice(
                status = status,
                customerId = customerId,
                amount = amount,
                id = (0..10000).random()
            )
        }
    }

}