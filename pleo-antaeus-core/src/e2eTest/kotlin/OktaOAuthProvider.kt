package io.pleo.antaeus.core.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import java.util.*

object OktaOAuthProvider {
    fun fetchToken(): OAuthResponse {
        val client = OkHttpClient()
        val requestBody: RequestBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("scope", "AntaeusAdmin")
            .build()
        val token = Base64.getEncoder().encodeToString(
            "0oabp6s3iJyiZhMMN5d6:cD42LkfdmgnMoNDqBfQPYIC5M3VFYlWqcTMFCoJ6".toByteArray(Charsets.UTF_8)
        )

        val request = Request.Builder()
            .url("https://dev-36600335.okta.com/oauth2/default/v1/token")
            .post(requestBody)
            .addHeader("authorization", "Basic $token")
            .build()

        val response: Response = client.newCall(request).execute()

        response.use {
            val om = ObjectMapper()
            val oauthData: OAuthResponse = om.readValue(response.body()?.string(), OAuthResponse::class.java)

            return oauthData
        }
    }
}