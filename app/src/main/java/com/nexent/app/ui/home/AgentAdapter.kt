package com.nexent.app.ui.home

import android.view.LayoutInflater
import android.view.View
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

    private var allAgents: List<Agent> = emptyList()
    private var searchQuery: String = ""
    private var recommendedOnly: Boolean = false

    // Map agent names to avatar background drawables for consistent colors
    private val avatarBgMap = mutableMapOf<String, Int>()
    private val avatarDrawables = intArrayOf(
        R.drawable.bg_avatar_01, R.drawable.bg_avatar_02,
        R.drawable.bg_avatar_03, R.drawable.bg_avatar_04,
        R.drawable.bg_avatar_05, R.drawable.bg_avatar_06,
        R.drawable.bg_avatar_07, R.drawable.bg_avatar_08
    )

    override fun submitList(list: List<Agent>?) {
        allAgents = list ?: emptyList()
        applyFilters()
    }

    fun filter(query: String) {
        searchQuery = query.trim()
        applyFilters()
    }

    fun filterRecommended(recommended: Boolean) {
        recommendedOnly = recommended
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allAgents

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        if (recommendedOnly) {
            filtered = filtered.filter { it.isRecommended }
        }

        super.submitList(filtered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentViewHolder {
        val binding = ItemAgentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AgentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgentViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class AgentViewHolder(private val binding: ItemAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(agent: Agent, @Suppress("UNUSED_PARAMETER") position: Int) {
            binding.tvAgentName.text = agent.title
            binding.tvAgentDescription.text = agent.description.ifBlank { agent.modelDisplayName }

            // Show author if available
            if (agent.author.isNotBlank()) {
                binding.tvAgentAuthor.visibility = View.VISIBLE
                binding.tvAgentAuthor.text = "@${agent.author}"
            } else {
                binding.tvAgentAuthor.visibility = View.GONE
            }

            // Assign consistent avatar color based on agent name hash
            val avatarBg = avatarBgMap.getOrPut(agent.name) {
                avatarDrawables[agent.name.hashCode().mod(avatarDrawables.size).let { if (it < 0) it + avatarDrawables.size else it }]
            }
            binding.flAvatar.setBackgroundResource(avatarBg)

            // Show recommend badge
            binding.tvBadge.visibility = if (agent.isRecommended) View.VISIBLE else View.GONE

            // Hide popularity for now
            binding.tvPopularity.visibility = View.GONE

            binding.root.setOnClickListener { onAgentClick(agent) }
        }
    }

    private class AgentDiffCallback : DiffUtil.ItemCallback<Agent>() {
        override fun areItemsTheSame(oldItem: Agent, newItem: Agent) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: Agent, newItem: Agent) = oldItem == newItem
    }
}
