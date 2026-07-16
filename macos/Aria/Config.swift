import Foundation

/// Supabase connection. Fill these with your project values (Supabase → Project
/// Settings → API). The anon key is a public client key; Row Level Security protects
/// data. For a real release, prefer injecting via an xcconfig / Info.plist instead of
/// hardcoding here.
enum Config {
    static let supabaseURL = SupaEnv.url
    static let supabaseAnonKey = SupaEnv.anonKey

    /// App Group shared with the widget (must match project.yml entitlements).
    static let appGroup = "group.com.aria.planner"

    static var isConfigured: Bool {
        !supabaseURL.contains("YOUR-PROJECT") && !supabaseAnonKey.contains("YOUR-ANON")
    }
}
