package com.nexent.app.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexent.app.databinding.ItemMessageAiBinding
import com.nexent.app.databinding.ItemMessageUserBinding

class MessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_AI = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemMessageUserBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            UserMessageViewHolder(binding)
        } else {
            val binding = ItemMessageAiBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            AiMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(item)
            is AiMessageViewHolder -> holder.bind(item)
        }
    }

    class UserMessageViewHolder(private val binding: ItemMessageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    class AiMessageViewHolder(private val binding: ItemMessageAiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem === newItem
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}
