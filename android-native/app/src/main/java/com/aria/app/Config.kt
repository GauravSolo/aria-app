package com.aria.app

/**
 * Supabase connection (same project as the Expo & Mac apps, so data syncs).
 * The anon key is a public client key; Row Level Security protects data.
 */
object Config {
    const val SUPABASE_URL = "https://nrfossdtwxfgyyszkado.supabase.co"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5yZm9zc2R0d3hmZ3l5c3prYWRvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3NTM4OTMsImV4cCI6MjA5OTMyOTg5M30.kG_CTyUEanNEQ6GMpeBeNlZ8lLUArAZhrIKyuJogjII"

    const val APP_GROUP = "aria_widget" // DataStore file name for the widget snapshot
}
