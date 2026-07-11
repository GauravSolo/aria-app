package com.aria.app.data

import android.content.Context
import com.aria.app.Config
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/** Global Supabase client, initialized once from the Application. */
object Supa {
    lateinit var client: SupabaseClient
        private set

    fun init(context: Context) {
        if (::client.isInitialized) return
        client = createSupabaseClient(
            supabaseUrl = Config.SUPABASE_URL,
            supabaseKey = Config.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                sessionManager = PrefsSessionManager(context.applicationContext)
                codeVerifierCache = MemoryCodeVerifierCache()
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
        }
    }
}
