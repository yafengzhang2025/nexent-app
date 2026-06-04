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

    init {
        // Load mock data for development
        loadMockAgents()
    }

    private fun loadMockAgents() {
        _agents.value = listOf(
            Agent("小宇宙", "小宇宙", "你的AI伙伴，随时为你答疑解惑", isRecommended = true),
            Agent("编程助手", "编程助手", "高效编程，Bug修复，代码优化", isRecommended = true),
            Agent("学习导师", "学习导师", "个性化学习计划，知识点讲解", isRecommended = true),
            Agent("文案大师", "文案大师", "爆款文案，一键生成", isRecommended = true),
            Agent("旅行规划", "旅行规划", "定制专属旅行攻略", isRecommended = true),
            Agent("情感陪伴", "情感陪伴", "倾听心事的AI伙伴", isRecommended = true),
            Agent("数据分析师", "数据分析师", "数据洞察，图表生成"),
            Agent("翻译官", "翻译官", "多语言实时翻译"),
            Agent("健身教练", "健身教练", "制定专属健身计划"),
            Agent("法律顾问", "法律顾问", "法律问题咨询解答"),
            Agent("面试导师", "面试导师", "模拟面试，简历优化"),
            Agent("儿童故事", "儿童故事", "睡前故事，寓教于乐")
        )
    }

    fun loadAgents() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val service = RetrofitClient.getInstance(prefHelper.baseUrl, prefHelper.apikey)
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
