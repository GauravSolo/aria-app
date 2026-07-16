import SwiftUI

struct RootView: View {
    @StateObject private var auth = AuthModel()
    @StateObject private var store = AppStore()
    @Environment(\.scenePhase) private var scenePhase
    @AppStorage("theme_mode") private var themeMode = ThemeMode.system.rawValue

    var body: some View {
        Group {
            switch auth.status {
            case .loading:
                ProgressView().frame(minWidth: 460, minHeight: 520)
            case .signedOut:
                AuthView().environmentObject(auth)
            case .signedIn:
                MainView()
                    .environmentObject(auth)
                    .environmentObject(store)
            }
        }
        .preferredColorScheme((ThemeMode(rawValue: themeMode) ?? .system).colorScheme)
        .task { await auth.bootstrap() }
        .task(id: auth.userId) {
            if let uid = auth.userId { await store.load(uid: uid) }
        }
        // Re-sync when the app becomes active — applies widget checkbox taps and
        // refreshes data (like the Android on-resume refresh).
        .onChange(of: scenePhase) { _, phase in
            if phase == .active, let uid = auth.userId {
                _Concurrency.Task { await store.load(uid: uid) }
            }
        }
    }
}

enum ThemeMode: String, CaseIterable, Identifiable {
    case system, light, dark
    var id: String { rawValue }
    var label: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

enum AppSection: String, CaseIterable, Identifiable {
    case today, planner, habits, water, profile
    var id: String { rawValue }
    var title: String {
        switch self {
        case .today: return "Today"
        case .planner: return "Planner"
        case .habits: return "Habits"
        case .water: return "Water"
        case .profile: return "Profile"
        }
    }
    var icon: String {
        switch self {
        case .today: return "sun.max"
        case .planner: return "checklist"
        case .habits: return "flame"
        case .water: return "drop"
        case .profile: return "person.crop.circle"
        }
    }
}

struct MainView: View {
    @EnvironmentObject var auth: AuthModel
    @EnvironmentObject var store: AppStore
    @State private var selection: AppSection = .today

    var body: some View {
        NavigationSplitView {
            List(AppSection.allCases, selection: $selection) { section in
                Label(section.title, systemImage: section.icon).tag(section)
            }
            .navigationSplitViewColumnWidth(min: 180, ideal: 200)
            .safeAreaInset(edge: .bottom) {
                HStack {
                    Image(systemName: "person.crop.circle").foregroundStyle(Brand.indigo)
                    Text(auth.email ?? "Account").font(.footnote).lineLimit(1)
                    Spacer()
                    Button { _Concurrency.Task { await auth.signOut() } } label: {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                    }.buttonStyle(.plain)
                }.padding(10)
            }
        } detail: {
            switch selection {
            case .today: DashboardView()
            case .planner: TasksView()
            case .habits: HabitsView()
            case .water: WaterView()
            case .profile: ProfileView()
            }
        }
        .frame(minWidth: 820, minHeight: 600)
        .alert("Sync error", isPresented: Binding(
            get: { store.lastError != nil },
            set: { if !$0 { store.lastError = nil } }
        )) {
            Button("OK") { store.lastError = nil }
        } message: {
            Text(store.lastError ?? "")
        }
    }
}
