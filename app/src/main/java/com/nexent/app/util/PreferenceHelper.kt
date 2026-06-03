package com.nexent.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    companion object {
        private const val PREF_NAME = "nexent_prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
    }
}
