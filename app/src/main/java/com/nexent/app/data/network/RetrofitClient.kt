package com.nexent.app.data.network

import com.nexent.app.data.model.ChatRequest
import com.nexent.app.data.model.ChatStreamChunk
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val AUTH_TOKEN = "nexent-6f1913254fb6a73d55d3254e"

    @Volatile
    private var instance: NexentApiService? = null

    @Volatile
    private var currentBaseUrl: String = ""

    private val gson = Gson()

    fun getInstance(baseUrl: String): NexentApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (instance == null || normalizedUrl != currentBaseUrl) {
            synchronized(this) {
                if (instance == null || normalizedUrl != currentBaseUrl) {
                    currentBaseUrl = normalizedUrl
                    instance = buildService(normalizedUrl)
                }
            }
        }
        return instance!!
    }

    private fun buildService(baseUrl: String): NexentApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NexentApiService::class.java)
    }

    /**
     * Execute a streaming chat request and emit chunks via a Channel.
     */
    suspend fun streamChat(
        baseUrl: String,
        request: ChatRequest
    ): Channel<ChatStreamChunk> = withContext(Dispatchers.IO) {
        val channel = Channel<ChatStreamChunk>(Channel.UNLIMITED)
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val jsonBody = gson.toJson(request)
        val reqBody = jsonBody.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("${normalizedUrl}nb/v1/chat/run")
            .post(reqBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $AUTH_TOKEN")
            .addHeader("Idempotency-Key", "idem-${UUID.randomUUID()}")
            .build()

        try {
            val response = client.newCall(httpRequest).execute()
            val source = response.body?.source() ?: run {
                channel.close()
                return@withContext channel
            }

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") {
                        channel.send(ChatStreamChunk(done = true))
                        break
                    }
                    try {
                        val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                        channel.send(chunk)
                    } catch (_: Exception) {
                        // Skip malformed JSON lines
                    }
                }
            }
        } catch (e: Exception) {
            channel.close(e)
            return@withContext channel
        }

        channel.close()
        channel
    }
}
