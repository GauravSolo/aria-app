import Foundation
import Supabase

/// Shared Supabase client, configured for snake_case ↔ camelCase conversion so the
/// Swift models in Models.swift map directly to the Postgres columns.
enum Supa {
    static let client: SupabaseClient = {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        return SupabaseClient(
            supabaseURL: URL(string: Config.supabaseURL)!,
            supabaseKey: Config.supabaseAnonKey,
            options: SupabaseClientOptions(
                db: SupabaseClientOptions.DatabaseOptions(encoder: encoder, decoder: decoder)
            )
        )
    }()
}

@MainActor
final class AuthModel: ObservableObject {
    enum Status { case loading, signedIn, signedOut }

    @Published var status: Status = .loading
    @Published var userId: String?
    @Published var email: String?
    @Published var busy = false
    @Published var error: String?

    func bootstrap() async {
        guard Config.isConfigured else { status = .signedOut; return }
        if let session = try? await Supa.client.auth.session {
            apply(session)
        } else {
            status = .signedOut
        }
        // Observe future auth changes. `authStateChanges` is actor-isolated in
        // current supabase-swift, so the property access itself must be awaited.
        _Concurrency.Task {
            for await change in await Supa.client.auth.authStateChanges {
                await MainActor.run { self.apply(change.session) }
            }
        }
    }

    private func apply(_ session: Session?) {
        if let session {
            userId = session.user.id.uuidString.lowercased()
            email = session.user.email
            status = .signedIn
            // Stash the session so the widget can sync checkbox taps to Supabase directly.
            WidgetBridge.writeAuth(WidgetAuth(
                accessToken: session.accessToken,
                refreshToken: session.refreshToken,
                userId: userId ?? "",
                expiresAt: session.expiresAt
            ))
        } else {
            userId = nil
            email = nil
            status = .signedOut
            WidgetBridge.clearAuth()
        }
    }

    func signIn(_ email: String, _ password: String) async {
        await run { try await Supa.client.auth.signIn(email: email, password: password) }
    }

    func signUp(_ email: String, _ password: String, name: String) async {
        await run {
            _ = try await Supa.client.auth.signUp(
                email: email,
                password: password,
                data: name.isEmpty ? nil : ["display_name": .string(name)]
            )
        }
    }

    func signOut() async {
        try? await Supa.client.auth.signOut()
        apply(nil)
    }

    private func run(_ work: @escaping () async throws -> Void) async {
        busy = true
        error = nil
        do { try await work() } catch { self.error = error.localizedDescription }
        busy = false
    }
}
