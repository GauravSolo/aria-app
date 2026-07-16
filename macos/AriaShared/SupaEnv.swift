import Foundation

/// Supabase connection constants shared by the app **and** the widget extension.
/// The anon key is a public client key; Row Level Security protects data.
enum SupaEnv {
    static let url = "https://nrfossdtwxfgyyszkado.supabase.co"
    static let anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5yZm9zc2R0d3hmZ3l5c3prYWRvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3NTM4OTMsImV4cCI6MjA5OTMyOTg5M30.kG_CTyUEanNEQ6GMpeBeNlZ8lLUArAZhrIKyuJogjII"
}
