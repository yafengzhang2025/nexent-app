package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("content") val content: String,
    @SerializedName("conversation_id") val conversationId: String
)
