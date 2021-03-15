package io.pleo.antaeus.core.e2e

import org.junit.jupiter.api.Test

class Auth {

    @Test
    fun `will call OAuth provider to fetch token`() {
        OktaOAuthProvider.fetchToken()
    }

}