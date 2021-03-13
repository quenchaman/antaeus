package io.pleo.antaeus.core.e2e

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface Api {
    @GET("/rest/health")
    fun checkHealth(): Call<ResponseBody>

    companion object {
        fun create(baseUrl: String): Api {
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