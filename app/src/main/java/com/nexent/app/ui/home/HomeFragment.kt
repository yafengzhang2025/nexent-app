package com.nexent.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexent.app.databinding.FragmentHomeBinding
import com.nexent.app.ui.chat.ChatActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var agentAdapter: AgentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadAgents()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadAgents()
        }
    }

    private fun setupRecyclerView() {
        agentAdapter = AgentAdapter { agent ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_AGENT_ID, agent.id)
                putExtra(ChatActivity.EXTRA_AGENT_NAME, agent.name)
            }
            startActivity(intent)
        }

        binding.rvAgents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = agentAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.agents.observe(viewLifecycleOwner) { agents ->
            agentAdapter.submitList(agents)
            binding.tvEmptyState.visibility = if (agents.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading && agentAdapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = error
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
