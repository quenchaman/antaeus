package io.pleo.antaeus.core.e2e

import io.pleo.antaeus.models.Invoice
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

const val authHeaderKey: String = "Authorization"

interface Api {

    @GET("/rest/health")
    fun checkHealth(@Header(authHeaderKey) authHeaderVal: String): Call<ResponseBody>

    @POST("rest/v1/invoices/charge")
    fun chargeInvoices(@Header(authHeaderKey) authHeaderVal: String): Call<ResponseBody>

    @GET("rest/v1/invoices")
    fun fetchAllInvoices(@Header(authHeaderKey) authHeaderVal: String): Call<List<Invoice>>

    companion object {
        fun create(): Api {
            val baseUrl: String = System.getProperty("apiUrl")
            val retrofit = Retrofit.Builder()
                // here we set the base url of our API
                .baseUrl(baseUrl)
                // add the JSON dependency so we can handle json APIs
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            // here we pass a reference to our API interface
            // and get back a concrete instance
            return retrofit.create(Api::class.java)
        }
    }
}