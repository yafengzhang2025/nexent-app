package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class AgentListResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: List<Agent> = emptyList()
)
