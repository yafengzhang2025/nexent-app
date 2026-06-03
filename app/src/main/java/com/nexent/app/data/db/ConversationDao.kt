package com.nexent.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE agentId = :agentId ORDER BY timestamp ASC")
    suspend fun getConversationsByAgent(agentId: String): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations WHERE agentId = :agentId")
    suspend fun clearConversation(agentId: String)
}
