package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class Agent(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)
