package com.nexent.app.data.model

import com.google.gson.annotations.SerializedName

data class Agent(
    @SerializedName("name") val name: String,
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("model_display_name") val modelDisplayName: String = ""
) {
    /** Display name to show in the UI */
    val title: String get() = displayName.ifBlank { name }
}
