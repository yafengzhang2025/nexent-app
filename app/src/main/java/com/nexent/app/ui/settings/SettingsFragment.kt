package com.nexent.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nexent.app.databinding.FragmentSettingsBinding
import com.nexent.app.util.PreferenceHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefHelper: PreferenceHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefHelper = PreferenceHelper(requireContext())

        binding.etBaseUrl.setText(prefHelper.baseUrl)
        binding.etApiKey.setText(prefHelper.apiKey)

        binding.btnSave.setOnClickListener {
            val baseUrl = binding.etBaseUrl.text?.toString()?.trim() ?: ""
            val apiKey = binding.etApiKey.text?.toString()?.trim() ?: ""

            if (baseUrl.isBlank()) {
                Toast.makeText(requireContext(), "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefHelper.baseUrl = baseUrl
            prefHelper.apiKey = apiKey
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
