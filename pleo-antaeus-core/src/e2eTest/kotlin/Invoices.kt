package io.pleo.antaeus.core.e2e

import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class Invoices {

    @Test
    fun `will call endpoint to charge unpaid invoices`() {
        val api = Api.create()
        val response = api.chargeInvoices().execute()

        Assertions.assertTrue(response.isSuccessful)

        runBlocking {
            // Give enough time for the previous call to at least lock the table and update invoice status
            delay(1000)
        }

        val allInvoicesResponse = api.fetchAllInvoices().execute()

        Assertions.assertTrue(allInvoicesResponse.isSuccessful)
        val invoices = allInvoicesResponse.body()

        Assertions.assertNotNull(invoices)

        invoices?.forEach { Assertions.assertTrue(it.status != InvoiceStatus.PENDING) }
    }

}