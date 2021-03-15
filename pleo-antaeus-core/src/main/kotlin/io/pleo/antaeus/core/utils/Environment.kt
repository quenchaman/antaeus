package io.pleo.antaeus.core.utils

object Environment {

    fun getOauthClientId(): String? {
        return System.getenv("OKTA_CLIENT_ID")
    }

    fun getOauthClientSecret(): String? {
        return System.getenv("OKTA_CLIENT_SECRET")
    }

    fun getOauthIssuer(): String? {
        return System.getenv("OAUTH_ISSUER")
    }

    fun getOauthScope(): String? {
        return System.getenv("OAUTH_SCOPE")
    }

}
