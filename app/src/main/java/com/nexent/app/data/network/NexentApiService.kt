package com.nexent.app.data.network

import com.nexent.app.data.model.Agent
import com.nexent.app.data.model.ChatRequest
import com.nexent.app.data.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NexentApiService {

    @GET("api/v1/agents")
    suspend fun getAgents(): List<Agent>

    @POST("api/v1/agents/{agentId}/chat")
    suspend fun chat(
        @Path("agentId") agentId: String,
        @Body request: ChatRequest
    ): ChatResponse
}
