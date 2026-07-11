package com.aria.app.data

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/** Persists the Supabase auth session in SharedPreferences (no extra deps). */
class PrefsSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("aria_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(KEY, json.encodeToString(UserSession.serializer(), session)).apply()
    }

    override suspend fun loadSession(): UserSession {
        val raw = prefs.getString(KEY, null) ?: throw NoSuchElementException("No stored session")
        return json.decodeFromString(UserSession.serializer(), raw)
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "session"
    }
}
