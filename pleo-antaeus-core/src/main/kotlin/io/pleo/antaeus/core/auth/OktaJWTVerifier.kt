package io.pleo.antaeus.core.auth

import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.JwtVerificationException
import com.okta.jwt.JwtVerifiers
import io.pleo.antaeus.core.utils.Environment

object OktaJWTVerifier {

    private val jwtVerifier: AccessTokenVerifier = JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(Environment.getOauthIssuer())
        .build()

    fun verify(jwt: String): Boolean {
        return try {
            jwtVerifier.decode(jwt)
            true
        } catch (e: JwtVerificationException) {
            false
        }
    }

}