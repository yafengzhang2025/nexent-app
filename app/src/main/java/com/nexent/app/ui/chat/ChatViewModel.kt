package com.nexent.app.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nexent.app.data.db.AppDatabase
import com.nexent.app.data.db.ConversationEntity
import com.nexent.app.data.model.ChatRequest
import com.nexent.app.data.network.RetrofitClient
import com.nexent.app.util.PreferenceHelper
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefHelper = PreferenceHelper(application)
    private val db = AppDatabase.getInstance(application)

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var conversationId: String? = null
    private var currentAgentId: String = ""

    fun init(agentId: String) {
        currentAgentId = agentId
        loadHistory(agentId)
    }

    private fun loadHistory(agentId: String) {
        viewModelScope.launch {
            val history = db.conversationDao().getConversationsByAgent(agentId)
            val chatMessages = history.map { entity ->
                ChatMessage(content = entity.message, isUser = entity.role == "user")
            }
            _messages.value = chatMessages
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val baseUrl = prefHelper.baseUrl
        if (baseUrl.isBlank()) {
            _error.value = "Server URL not configured."
            return
        }

        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(ChatMessage(userText, isUser = true))
        _messages.value = currentMessages.toList()

        viewModelScope.launch {
            db.conversationDao().insert(
                ConversationEntity(agentId = currentAgentId, message = userText, role = "user")
            )
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val service = RetrofitClient.getInstance(baseUrl, prefHelper.apiKey)
                val response = service.chat(
                    currentAgentId,
                    ChatRequest(message = userText, conversationId = conversationId)
                )
                conversationId = response.conversationId

                val updatedMessages = _messages.value.orEmpty().toMutableList()
                updatedMessages.add(ChatMessage(response.content, isUser = false))
                _messages.value = updatedMessages.toList()

                db.conversationDao().insert(
                    ConversationEntity(
                        agentId = currentAgentId,
                        message = response.content,
                        role = "assistant"
                    )
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send message"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
