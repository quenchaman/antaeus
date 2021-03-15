package io.pleo.antaeus.core.e2e

data class OAuthResponse(val token_type: String = "", val expires_in: Int = 0, val access_token: String = "", val scope: String = "") {
}