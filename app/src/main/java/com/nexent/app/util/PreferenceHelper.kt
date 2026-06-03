package com.nexent.app.util

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "nexent_prefs"
        private const val KEY_HOST = "server_host"
        private const val KEY_PORT = "server_port"
        const val DEFAULT_HOST = "60.204.251.153"
        const val DEFAULT_PORT = "5013"
    }

    var host: String
        get() = prefs.getString(KEY_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
        set(value) = prefs.edit().putString(KEY_HOST, value).apply()

    var port: String
        get() = prefs.getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
        set(value) = prefs.edit().putString(KEY_PORT, value).apply()

    val baseUrl: String
        get() {
            val h = host.ifBlank { DEFAULT_HOST }
            val p = port.ifBlank { DEFAULT_PORT }
            return "http://$h:$p"
        }
}
