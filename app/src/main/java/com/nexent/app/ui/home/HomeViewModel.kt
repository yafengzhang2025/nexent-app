package com.nexent.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nexent.app.data.model.Agent
import com.nexent.app.data.network.RetrofitClient
import com.nexent.app.util.PreferenceHelper
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefHelper = PreferenceHelper(application)

    private val _agents = MutableLiveData<List<Agent>>()
    val agents: LiveData<List<Agent>> = _agents

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadAgents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val service = RetrofitClient.getInstance(prefHelper.baseUrl)
                val response = service.getAgents()
                _agents.value = response.data
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
