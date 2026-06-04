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
    val thinkingContent: String = "",
    val isUser: Boolean,
    val imageUri: String? = null,
    val isStreaming: Boolean = false,
    val isDeepThink: Boolean = false
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
    private var deepThinkEnabled: Boolean = false

    private val _thinkMode = MutableLiveData<String>("quick")
    val thinkMode: LiveData<String> = _thinkMode

    fun init(agentName: String) {
        currentAgentName = agentName
    }

    fun setThinkMode(mode: String) {
        _thinkMode.value = mode
        deepThinkEnabled = mode == "deep"
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
            query = text,
            deepThink = deepThinkEnabled
        )

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Add a placeholder AI message for streaming
            val aiMsg = ChatMessage(
                content = "",
                isUser = false,
                isStreaming = true,
                isDeepThink = deepThinkEnabled
            )
            val updated = _messages.value.orEmpty().toMutableList()
            updated.add(aiMsg)
            _messages.value = updated.toList()

            try {
                val channel: Channel<ChatStreamChunk> = RetrofitClient.streamChat(baseUrl, request, prefHelper.apikey)
                var fullRaw = StringBuilder()
                var thinkingContent = StringBuilder()
                var answerContent = StringBuilder()
                var inThinking = false

                for (chunk in channel) {
                    if (chunk.done) break
                    val raw = chunk.content
                    fullRaw.append(raw)
                    chunk.conversationId?.let { conversationId = it }

                    // Detect thinking markers (common patterns: 思考, thinking, etc.)
                    if (raw.contains("思考") || raw.contains("thinking") || raw.contains("分析")) {
                        inThinking = true
                    }

                    if (inThinking && (raw.contains("回答") || raw.contains("答案") || raw.contains("---"))) {
                        inThinking = false
                        // Add the rest to answer
                        val clean = sanitizeContent(raw)
                        answerContent.append(clean)
                    } else if (inThinking) {
                        thinkingContent.append(sanitizeContent(raw))
                    } else {
                        answerContent.append(sanitizeContent(raw))
                    }

                    // Update the streaming message
                    val msgs = _messages.value.orEmpty().toMutableList()
                    val lastIdx = msgs.lastIndex
                    if (lastIdx >= 0 && msgs[lastIdx].isStreaming) {
                        msgs[lastIdx] = msgs[lastIdx].copy(
                            content = answerContent.toString().ifBlank { sanitizeContent(fullRaw.toString()) },
                            thinkingContent = thinkingContent.toString()
                        )
                        _messages.value = msgs.toList()
                    }
                }

                // Mark streaming complete
                val finalMsgs = _messages.value.orEmpty().toMutableList()
                val last = finalMsgs.lastIndex
                if (last >= 0 && finalMsgs[last].isStreaming) {
                    val cleanAnswer = sanitizeContent(answerContent.toString().ifBlank { fullRaw.toString() })
                    finalMsgs[last] = finalMsgs[last].copy(
                        content = cleanAnswer,
                        thinkingContent = sanitizeContent(thinkingContent.toString()),
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

    companion object {
        /** Remove MCP/metadata markers and clean up stream output */
        fun sanitizeContent(text: String): String {
            return text
                .replace(Regex("MCP_START.*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("MCP_END.*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("MCP_CALL.*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("TOOL_START.*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("TOOL_END.*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<tool_call>.*?</tool_call>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<function_call>.*?</function_call>", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("\\[MCP_.*?\\]", RegexOption.IGNORE_CASE), "")
                .replace(Regex("(?m)^\\s*$\\n?", RegexOption.MULTILINE), "")
                .trim()
        }
    }
}
