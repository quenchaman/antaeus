package io.pleo.antaeus.core.e2e

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class Health {
    @Test
    fun `will call endpoint to fetch health status`() {
        val api = Api.create("http://localhost:7000")
        val response = api.checkHealth().execute()

        Assertions.assertEquals(200, response.code())
    }
}
