package com.aria.app.data

import android.content.Context
import com.aria.app.Config
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

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
            // encodeDefaults=true so null/default fields (e.g. deleted_at=null when
            // un-deleting, is_completed=false when unchecking) are actually sent in
            // upserts — otherwise those changes silently don't persist.
            defaultSerializer = KotlinXSerializer(Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            })
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
