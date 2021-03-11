package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import java.math.BigDecimal

class ExchangeProviderImpl : ExchangeProvider {
    override fun exchange(amount: BigDecimal, currency: Currency, targetCurrency: Currency): BigDecimal {
        TODO("Not yet implemented")
    }
}