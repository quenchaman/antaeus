package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import java.math.BigDecimal

interface ExchangeProvider {

    /*
        Exchange amount of money from one currency to another.

        Returns:
          The exchanged amount of money.

        Throws:
          `NetworkException`: when a network error happens.
     */
    fun exchange(amount: BigDecimal, currency: Currency, targetCurrency: Currency): BigDecimal

}