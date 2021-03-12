
import io.pleo.antaeus.core.external.ExchangeProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(invoiceDal: InvoiceDal, customerDal: CustomerDal) {
    val customers = (1..100).mapNotNull {
        customerDal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            invoiceDal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return Random.nextBoolean()
        }
    }
}

internal fun getExchangeProvider(): ExchangeProvider {
    return object : ExchangeProvider {
        override fun exchange(amount: BigDecimal, currency: Currency, targetCurrency: Currency): BigDecimal {
            return BigDecimal.TEN
        }
    }
}
