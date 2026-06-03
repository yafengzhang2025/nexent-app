package com.nexent.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nexent.app.R
import com.nexent.app.data.model.Agent
import com.nexent.app.databinding.ItemAgentBinding

class AgentAdapter(
    private val onStartChat: (Agent) -> Unit
) : ListAdapter<Agent, AgentAdapter.AgentViewHolder>(AgentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentViewHolder {
        val binding = ItemAgentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AgentViewHolder(private val binding: ItemAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(agent: Agent) {
            binding.tvAgentName.text = agent.name
            binding.tvAgentDescription.text = agent.description

            if (!agent.avatarUrl.isNullOrBlank()) {
                Glide.with(binding.ivAvatar.context)
                    .load(agent.avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            }

            binding.btnStartChat.setOnClickListener { onStartChat(agent) }
        }
    }

    private class AgentDiffCallback : DiffUtil.ItemCallback<Agent>() {
        override fun areItemsTheSame(oldItem: Agent, newItem: Agent) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Agent, newItem: Agent) = oldItem == newItem
    }
}
