package io.pleo.antaeus.core.auth

import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.Jwt
import com.okta.jwt.JwtVerificationException
import com.okta.jwt.JwtVerifiers

object OktaJWTVerifier {

    private val jwtVerifier: AccessTokenVerifier = JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(
//            System.getenv("OAUTH_ISSUER")
        "https://dev-36600335.okta.com/oauth2/default"
        )
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