package com.wardpulse.app.network

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("wardpulse_prefs", Context.MODE_PRIVATE)

    fun saveSession(token: String, hospitalName: String, serverUrl: String) {
        prefs.edit()
            .putString("jwt_token", token)
            .putString("hospital_name", hospitalName)
            .putString("server_url", serverUrl)
            .apply()
    }

    fun getBearerToken(): String? {
        val raw = prefs.getString("jwt_token", null) ?: return null
        return if (raw.startsWith("Bearer ", ignoreCase = true)) raw else "Bearer $raw"
    }

    fun getHospitalName(): String = prefs.getString("hospital_name", "General Hospital") ?: "General Hospital"

    fun getServerUrl(): String = prefs.getString("server_url", "http://192.168.43.1:8000") ?: "http://192.168.43.1:8000"

    fun saveServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
    }

    fun isLoggedIn(): Boolean = !getBearerToken().isNullOrBlank()

    fun logout() {
        prefs.edit().remove("jwt_token").remove("hospital_name").apply()
    }
}