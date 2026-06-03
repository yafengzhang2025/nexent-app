package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("conversation_id") val conversationId: Int = 1,
    @SerializedName("agent_name") val agentName: String,
    @SerializedName("query") val query: String
)
