package com.nexent.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexent.app.R
import com.nexent.app.data.model.Agent
import com.nexent.app.databinding.ItemAgentBinding

class AgentAdapter(
    private val onAgentClick: (Agent) -> Unit
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
            binding.tvAgentName.text = agent.title
            binding.tvAgentDescription.text = agent.description.ifBlank { agent.modelDisplayName }
            binding.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
            binding.root.setOnClickListener { onAgentClick(agent) }
        }
    }

    private class AgentDiffCallback : DiffUtil.ItemCallback<Agent>() {
        override fun areItemsTheSame(oldItem: Agent, newItem: Agent) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Agent, newItem: Agent) = oldItem == newItem
    }
}
