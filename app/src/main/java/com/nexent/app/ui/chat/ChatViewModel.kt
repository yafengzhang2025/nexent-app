package com.nexent.app.ui.chat

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nexent.app.data.model.ChatRequest
import com.nexent.app.data.model.ChatStreamChunk
import com.nexent.app.data.network.RetrofitClient
import com.nexent.app.util.PreferenceHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val content: String,
    val isUser: Boolean,
    val imageUri: String? = null,
    val isStreaming: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefHelper = PreferenceHelper(application)

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var conversationId: Int = 1
    private var currentAgentName: String = ""

    fun init(agentName: String) {
        currentAgentName = agentName
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(ChatMessage(content = text, isUser = true))
        _messages.value = currentMessages.toList()

        val baseUrl = prefHelper.baseUrl
        val request = ChatRequest(
            conversationId = conversationId,
            agentName = currentAgentName,
            query = text
        )

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Add a placeholder AI message for streaming
            val aiMsg = ChatMessage(content = "", isUser = false, isStreaming = true)
            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(aiMsg)
            _messages.value = updated.toList()

            try {
                val channel: Channel<ChatStreamChunk> = RetrofitClient.streamChat(baseUrl, request)
                var fullContent = StringBuilder()

                for (chunk in channel) {
                    if (chunk.done) break
                    fullContent.append(chunk.content)
                    chunk.conversationId?.let { conversationId = it }

                    // Update the streaming message
                    val msgs = _messages.value.orEmpty().toMutableList()
                    val lastIdx = msgs.lastIndex
                    if (lastIdx >= 0 && msgs[lastIdx].isStreaming) {
                        msgs[lastIdx] = msgs[lastIdx].copy(content = fullContent.toString())
                        _messages.value = msgs.toList()
                    }
                }

                // Mark streaming complete
                val finalMsgs = _messages.value.orEmpty().toMutableList()
                val last = finalMsgs.lastIndex
                if (last >= 0 && finalMsgs[last].isStreaming) {
                    finalMsgs[last] = finalMsgs[last].copy(
                        content = fullContent.toString(),
                        isStreaming = false
                    )
                    _messages.value = finalMsgs.toList()
                }
            } catch (e: Exception) {
                // Remove the streaming placeholder on error
                val errMsgs = _messages.value.orEmpty().toMutableList()
                if (errMsgs.isNotEmpty() && errMsgs.last().isStreaming) {
                    errMsgs.removeLast()
                    _messages.value = errMsgs.toList()
                }
                _error.value = "发送失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendImageMessage(imageUri: Uri, description: String) {
        val currentMessages = _messages.value.orEmpty().toMutableList()
        currentMessages.add(
            ChatMessage(
                content = description.ifBlank { "[图片]" },
                isUser = true,
                imageUri = imageUri.toString()
            )
        )
        _messages.value = currentMessages.toList()

        // Also send the description as text query
        sendMessage(description.ifBlank { "请分析这张图片" })
    }
}
