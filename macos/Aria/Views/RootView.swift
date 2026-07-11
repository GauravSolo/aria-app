import SwiftUI

struct RootView: View {
    @StateObject private var auth = AuthModel()
    @StateObject private var store = AppStore()

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
        .task { await auth.bootstrap() }
        .task(id: auth.userId) {
            if let uid = auth.userId { await store.load(uid: uid) }
        }
    }
}

enum AppSection: String, CaseIterable, Identifiable {
    case today, planner, habits, water
    var id: String { rawValue }
    var title: String {
        switch self {
        case .today: return "Today"
        case .planner: return "Planner"
        case .habits: return "Habits"
        case .water: return "Water"
        }
    }
    var icon: String {
        switch self {
        case .today: return "sun.max"
        case .planner: return "checklist"
        case .habits: return "flame"
        case .water: return "drop"
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
                    Button { Foundation.Task { await auth.signOut() } } label: {
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
            }
        }
        .frame(minWidth: 820, minHeight: 600)
    }
}
