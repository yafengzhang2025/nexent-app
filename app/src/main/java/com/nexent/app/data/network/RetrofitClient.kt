package com.nexent.app.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var instance: NexentApiService? = null

    @Volatile
    private var currentBaseUrl: String = ""

    @Volatile
    private var currentApiKey: String = ""

    fun getInstance(baseUrl: String, apiKey: String): NexentApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (instance == null || normalizedUrl != currentBaseUrl || apiKey != currentApiKey) {
            synchronized(this) {
                if (instance == null || normalizedUrl != currentBaseUrl || apiKey != currentApiKey) {
                    currentBaseUrl = normalizedUrl
                    currentApiKey = apiKey
                    instance = buildService(normalizedUrl, apiKey)
                }
            }
        }
        return instance!!
    }

    private fun buildService(baseUrl: String, apiKey: String): NexentApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .addHeader("Content-Type", "application/json")
                if (apiKey.isNotBlank()) {
                    requestBuilder.addHeader("X-API-Key", apiKey)
                }
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NexentApiService::class.java)
    }
}
