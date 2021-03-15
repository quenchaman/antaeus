package io.pleo.antaeus.core.e2e

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class Health {
    @Test
    fun `will call endpoint to fetch health status`() {
        val oauthData: OAuthResponse = OktaOAuthProvider.fetchToken()
        val api = Api.create()
        val response = api.checkHealth("Bearer ${oauthData.access_token}").execute()

        Assertions.assertTrue(response.isSuccessful)
    }

    @Test
    fun `will not allow unauthorized access`() {
        val api = Api.create()
        val response = api.checkHealth("Bieber it is a token man...come on..let me through").execute()

        Assertions.assertEquals(401, response.code())
    }
}
