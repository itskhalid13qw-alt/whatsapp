package com.messagestudio.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App settings. Nothing secret ships inside the app: the backend address (and an optional access
 * token for YOUR backend) are typed in by the user and kept only on this device. The real AI
 * provider key lives on the backend server, never here.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("studio_settings", Context.MODE_PRIVATE)

    var backendUrl by mutableStateOf(prefs.getString("backendUrl", "").orEmpty())
        private set
    var authToken by mutableStateOf(prefs.getString("authToken", "").orEmpty())
        private set
    var aiConsent by mutableStateOf(prefs.getBoolean("aiConsent", false))
        private set
    var batchSize by mutableIntStateOf(prefs.getInt("batchSize", 25))
        private set
    var warnAt by mutableIntStateOf(prefs.getInt("warnAt", 100))
        private set
    /** Maximum AI messages per day. 0 means no limit. */
    var dailyLimit by mutableIntStateOf(prefs.getInt("dailyLimit", 0))
        private set
    var shareWarned by mutableStateOf(prefs.getBoolean("shareWarned", false))
        private set

    fun setBackendUrl(v: String) { backendUrl = v.trim(); prefs.edit().putString("backendUrl", backendUrl).apply() }
    fun setAuthToken(v: String) { authToken = v.trim(); prefs.edit().putString("authToken", authToken).apply() }
    fun setAiConsent(v: Boolean) { aiConsent = v; prefs.edit().putBoolean("aiConsent", v).apply() }
    fun setBatchSize(v: Int) { batchSize = v.coerceIn(5, 50); prefs.edit().putInt("batchSize", batchSize).apply() }
    fun setWarnAt(v: Int) { warnAt = v.coerceAtLeast(1); prefs.edit().putInt("warnAt", warnAt).apply() }
    fun setDailyLimit(v: Int) { dailyLimit = v.coerceAtLeast(0); prefs.edit().putInt("dailyLimit", dailyLimit).apply() }
    fun setShareWarned(v: Boolean) { shareWarned = v; prefs.edit().putBoolean("shareWarned", v).apply() }
}
