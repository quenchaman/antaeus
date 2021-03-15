package io.pleo.antaeus.core.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import io.pleo.antaeus.core.utils.Environment
import okhttp3.*
import java.util.*

object OktaOAuthProvider {
    fun fetchToken(
        clientId: String? = Environment.getOauthClientId() ?: "",
        clientSecret: String? = Environment.getOauthClientSecret() ?: "",
        issuer: String? = Environment.getOauthIssuer() ?: "",
        scope: String? = Environment.getOauthScope() ?: ""
    ): OAuthResponse {
        val client = OkHttpClient()
        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", scope)
            .build()
        val token = "${clientId}:${clientSecret}".toByteArray(Charsets.UTF_8)
        val tokenBase64 = Base64.getEncoder().encodeToString(token)

        val request = Request.Builder()
            .url("${issuer}/v1/token")
            .post(requestBody)
            .addHeader("authorization", "Basic $tokenBase64")
            .build()

        val response: Response = client.newCall(request).execute()

        response.use {
            val om = ObjectMapper()
            val oauthData: OAuthResponse = om.readValue(response.body()?.string(), OAuthResponse::class.java)

            return oauthData
        }
    }
}