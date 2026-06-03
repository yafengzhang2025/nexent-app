package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("conversation_id") val conversationId: String? = null
)
