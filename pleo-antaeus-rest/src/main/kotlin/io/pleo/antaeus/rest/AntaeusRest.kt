/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import com.okta.jwt.Jwt
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.auth.OktaJWTVerifier
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start((System.getenv("port") ?: "3000").toInt())
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            before { ctx ->
                run {
                    val authHeaderVal = ctx.header("authorization")
                    val unauthorizedMessage = "Missing or invalid token"
                    val unauthorizedCode = 401

                    if (authHeaderVal == null) {
                        ctx.status(unauthorizedCode).result(unauthorizedMessage)
                    } else {
                        val authHeaderParts: List<String> = authHeaderVal.split(" ")

                        if (authHeaderParts.size == 2) {
                            if (!OktaJWTVerifier.verify(authHeaderParts[1])) {
                                ctx.status(unauthorizedCode).result(unauthorizedMessage)
                            } else {}
                        } else {
                            ctx.status(unauthorizedCode).result(unauthorizedMessage)
                        }
                    }
                }
            }

            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }

            exception(Exception::class.java) { e, ctx ->
                logger.error(e) { "Internal server error: " + e.printStackTrace() }
                ctx.status(500)
            }

            error(404) { ctx -> ctx.json("not found") }
            error(500) { ctx -> ctx.json("internal server error")}
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                get("health") {
                    it.json("ok")
                }

                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }

                        // URL: /rest/v1/invoices/charge
                        post("charge") {
                            GlobalScope.launch {
                                billingService.charge()
                            }
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }
                }
            }
        }
    }
}
