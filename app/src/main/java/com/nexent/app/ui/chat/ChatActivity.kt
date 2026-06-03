package com.nexent.app.ui.chat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexent.app.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val agentId = intent.getStringExtra(EXTRA_AGENT_ID) ?: ""
        val agentName = intent.getStringExtra(EXTRA_AGENT_NAME) ?: "AI Assistant"

        setupToolbar(agentName)
        setupRecyclerView()
        setupInputArea()
        observeViewModel()

        viewModel.init(agentId)
    }

    private fun setupToolbar(agentName: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = agentName
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.setText("")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages.toList()) {
                if (messages.isNotEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarChat.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_AGENT_ID = "extra_agent_id"
        const val EXTRA_AGENT_NAME = "extra_agent_name"
    }
}
