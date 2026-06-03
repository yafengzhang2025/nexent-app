package com.nexent.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: String,
    val message: String,
    val role: String,
    val timestamp: Long = System.currentTimeMillis()
)
