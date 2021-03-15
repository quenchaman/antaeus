package io.pleo.antaeus.core.utils

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice

fun getTestPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            if (invoice.customerId != 1) {
                throw CustomerNotFoundException(invoice.customerId)
            }

            return true
        }
    }
}
