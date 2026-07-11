import Foundation

/// Supabase connection. Fill these with your project values (Supabase → Project
/// Settings → API). The anon key is a public client key; Row Level Security protects
/// data. For a real release, prefer injecting via an xcconfig / Info.plist instead of
/// hardcoding here.
enum Config {
    static let supabaseURL = "https://nrfossdtwxfgyyszkado.supabase.co"
    static let supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5yZm9zc2R0d3hmZ3l5c3prYWRvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM3NTM4OTMsImV4cCI6MjA5OTMyOTg5M30.kG_CTyUEanNEQ6GMpeBeNlZ8lLUArAZhrIKyuJogjII"

    /// App Group shared with the widget (must match project.yml entitlements).
    static let appGroup = "group.com.aria.planner"

    static var isConfigured: Bool {
        !supabaseURL.contains("YOUR-PROJECT") && !supabaseAnonKey.contains("YOUR-ANON")
    }
}
