package com.nexent.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexent.app.R
import com.nexent.app.databinding.FragmentHomeBinding
import com.nexent.app.ui.chat.ChatActivity
import com.nexent.app.util.PreferenceHelper

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var agentAdapter: AgentAdapter
    private lateinit var prefHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefHelper = PreferenceHelper(requireContext())
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadAgents()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadAgents()
        }
    }

    private fun setupToolbar() {
        binding.toolbarHome.inflateMenu(R.menu.home_toolbar_menu)
        binding.toolbarHome.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_server_config -> {
                    showServerConfigDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showServerConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_config, null)
        val etHost = dialogView.findViewById<EditText>(R.id.et_host)
        val etPort = dialogView.findViewById<EditText>(R.id.et_port)

        etHost.setText(prefHelper.host)
        etPort.setText(prefHelper.port)

        AlertDialog.Builder(requireContext())
            .setTitle("服务器配置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val host = etHost.text.toString().trim()
                val port = etPort.text.toString().trim()
                if (host.isNotBlank() && port.isNotBlank()) {
                    prefHelper.host = host
                    prefHelper.port = port
                    Toast.makeText(requireContext(), "配置已保存", Toast.LENGTH_SHORT).show()
                    viewModel.loadAgents()
                } else {
                    Toast.makeText(requireContext(), "IP和端口不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupRecyclerView() {
        agentAdapter = AgentAdapter { agent ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_AGENT_NAME, agent.name)
                putExtra(ChatActivity.EXTRA_AGENT_DESC, agent.description)
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
