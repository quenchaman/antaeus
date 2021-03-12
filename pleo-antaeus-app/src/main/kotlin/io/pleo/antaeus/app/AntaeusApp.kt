/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.ExchangeService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.external.getExchangeProvider
import io.pleo.antaeus.core.external.getPaymentProvider
import io.pleo.antaeus.data.*
import io.pleo.antaeus.rest.AntaeusRest

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)
    val db = DBConnection.connect("antaeus-db", tables)

    // Set up data access layer.
    val invoiceDal = InvoiceDal(db = db)
    val customerDal = CustomerDal(db = db)

    // Insert example data in the database.
    setupInitialData(invoiceDal = invoiceDal, customerDal = customerDal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = invoiceDal)
    val customerService = CustomerService(dal = customerDal)
    val exchangeService = ExchangeService(getExchangeProvider())

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        exchangeService = exchangeService
    )

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        billingService = billingService
    ).run()
}
