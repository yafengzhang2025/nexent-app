package com.nexent.app.data.network

import com.nexent.app.data.model.AgentListResponse
import retrofit2.http.GET

interface NexentApiService {

    @GET("nb/v1/agents")
    suspend fun getAgents(): AgentListResponse
}
