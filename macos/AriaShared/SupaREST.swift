import Foundation

/// Minimal PostgREST + auth-refresh client over `URLSession`, used by the **widget
/// extension** so a checkbox tap writes to Supabase immediately (instant sync) rather
/// than waiting for the app to open. The app itself uses `supabase-swift` instead.
enum SupaREST {
    private static let restBase = "\(SupaEnv.url)/rest/v1"
    private static let authBase = "\(SupaEnv.url)/auth/v1"

    /// A valid access token, refreshing the stashed session if it's expired/near expiry.
    static func validToken() async -> String? {
        guard var a = WidgetBridge.readAuth() else { return nil }
        if a.expiresAt - Date().timeIntervalSince1970 > 60 { return a.accessToken }
        guard let r = try? await refresh(a.refreshToken) else { return nil }
        a.accessToken = r.access; a.refreshToken = r.refresh; a.expiresAt = r.expiresAt
        WidgetBridge.saveAuthLocal(a)
        return a.accessToken
    }

    private static func refresh(_ token: String) async throws -> (access: String, refresh: String, expiresAt: Double) {
        var req = URLRequest(url: URL(string: "\(authBase)/token?grant_type=refresh_token")!)
        req.httpMethod = "POST"
        req.setValue(SupaEnv.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONSerialization.data(withJSONObject: ["refresh_token": token])
        let (data, _) = try await URLSession.shared.data(for: req)
        let j = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        guard let access = j["access_token"] as? String, let refresh = j["refresh_token"] as? String else {
            throw URLError(.userAuthenticationRequired)
        }
        let expiresIn = (j["expires_in"] as? Double) ?? 3600
        return (access, refresh, Date().timeIntervalSince1970 + expiresIn)
    }

    /// Mark a task done from the widget. Non-recurring → update row; recurring → insert a
    /// completion for today. Returns true only on a 2xx write.
    static func completeTask(id: String, recurrence: String, userId: String) async -> Bool {
        guard let token = await validToken() else { return false }
        let now = isoNow()
        if recurrence == "none" {
            return await write(method: "PATCH", path: "tasks?id=eq.\(id)", token: token,
                               body: ["is_completed": true, "completed_at": now, "updated_at": now])
        } else {
            // Upsert on (task_id, occurrence_date): a prior toggle leaves a soft-deleted
            // row, and the table has a unique constraint — a plain INSERT would 409. Merge
            // instead and clear deleted_at so the completion counts.
            let row: [String: Any] = [
                "user_id": userId, "task_id": id, "occurrence_date": todayKey(),
                "completed_at": now, "updated_at": now, "deleted_at": NSNull(),
            ]
            return await write(method: "POST", path: "task_completions?on_conflict=task_id,occurrence_date",
                               token: token, body: row, prefer: "return=minimal,resolution=merge-duplicates")
        }
    }

    private static func write(method: String, path: String, token: String, body: Any,
                              prefer: String = "return=minimal") async -> Bool {
        guard let url = URL(string: "\(restBase)/\(path)"),
              let data = try? JSONSerialization.data(withJSONObject: body) else { return false }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue(SupaEnv.anonKey, forHTTPHeaderField: "apikey")
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue(prefer, forHTTPHeaderField: "Prefer")
        req.httpBody = data
        guard let (_, resp) = try? await URLSession.shared.data(for: req),
              let code = (resp as? HTTPURLResponse)?.statusCode else { return false }
        return (200...299).contains(code)
    }

    private static func isoNow() -> String {
        let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f.string(from: Date())
    }

    private static func todayKey() -> String {
        let f = DateFormatter(); f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy-MM-dd"; f.timeZone = .current
        return f.string(from: Date())
    }
}
