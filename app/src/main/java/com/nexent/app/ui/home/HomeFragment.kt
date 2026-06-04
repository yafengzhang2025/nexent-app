package com.nexent.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
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
    private var isGridLayout = true
    private var isRecommendedTab = false

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
        setupHeader()
        setupRecyclerView()
        setupSearch()
        setupTabs()
        observeViewModel()
        viewModel.loadAgents()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadAgents()
        }
    }

    private fun setupHeader() {
        binding.btnSettings.setOnClickListener { showServerConfigDialog() }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                agentAdapter.filter(query)
            }
        })
    }

    private fun setupTabs() {
        binding.tabAll.setOnClickListener { selectTab(false) }
        binding.tabRecommended.setOnClickListener { selectTab(true) }
        selectTab(false)
    }

    private fun selectTab(recommended: Boolean) {
        isRecommendedTab = recommended
        if (recommended) {
            binding.tabAll.setTextColor(resources.getColor(R.color.text_secondary, null))
            binding.tabAll.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.tabRecommended.setTextColor(resources.getColor(R.color.primary, null))
            binding.tabRecommended.setTypeface(null, android.graphics.Typeface.BOLD)
            // Animate tab indicator
            binding.tabIndicator.animate()
                .translationX(binding.tabRecommended.x - binding.tabAll.x + 24.dpToPx())
                .setDuration(200)
                .start()
        } else {
            binding.tabAll.setTextColor(resources.getColor(R.color.primary, null))
            binding.tabAll.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tabRecommended.setTextColor(resources.getColor(R.color.text_secondary, null))
            binding.tabRecommended.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.tabIndicator.animate()
                .translationX(0f)
                .setDuration(200)
                .start()
        }
        agentAdapter.filterRecommended(recommended)
    }

    private fun Int.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    private fun showServerConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_server_config, null)
        val etHost = dialogView.findViewById<EditText>(R.id.et_host)
        val etPort = dialogView.findViewById<EditText>(R.id.et_port)
        val etApikey = dialogView.findViewById<EditText>(R.id.et_apikey)

        etHost.setText(prefHelper.host)
        etPort.setText(prefHelper.port)
        etApikey.setText(prefHelper.apikey)

        AlertDialog.Builder(requireContext())
            .setTitle("服务器配置")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val host = etHost.text.toString().trim()
                val port = etPort.text.toString().trim()
                val apikey = etApikey.text.toString().trim()
                if (host.isNotBlank() && port.isNotBlank()) {
                    prefHelper.host = host
                    prefHelper.port = port
                    prefHelper.apikey = apikey
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
                putExtra(ChatActivity.EXTRA_AGENT_TITLE, agent.title)
                putExtra(ChatActivity.EXTRA_AGENT_DESC, agent.description)
            }
            startActivity(intent)
        }

        binding.rvAgents.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
