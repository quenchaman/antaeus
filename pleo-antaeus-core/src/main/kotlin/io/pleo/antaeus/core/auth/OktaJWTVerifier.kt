package io.pleo.antaeus.core.auth

import com.okta.jwt.AccessTokenVerifier
import com.okta.jwt.Jwt
import com.okta.jwt.JwtVerifiers

object OktaJWTVerifier {

    private val jwtVerifier: AccessTokenVerifier = JwtVerifiers.accessTokenVerifierBuilder()
        .setIssuer(System.getenv("OAUTH_ISSUER"))
        .build()

    fun verify(jwt: String): Jwt {
        return jwtVerifier.decode(jwt)
    }

}