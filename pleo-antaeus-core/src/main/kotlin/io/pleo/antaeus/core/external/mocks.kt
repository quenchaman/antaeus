package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import java.math.BigDecimal
import kotlin.random.Random

fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            return Random.nextBoolean()
        }
    }
}

fun getExchangeProvider(): ExchangeProvider {
    return object : ExchangeProvider {
        override fun exchange(amount: BigDecimal, currency: Currency, targetCurrency: Currency): BigDecimal {
            return BigDecimal.TEN
        }
    }
}
