import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var auth: AuthModel
    @AppStorage("theme_mode") private var themeMode = ThemeMode.system.rawValue

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                SectionTitle(text: "Profile")
                Card {
                    HStack(spacing: 12) {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.largeTitle).foregroundStyle(Brand.indigo)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(auth.email ?? "Signed in").font(.headline)
                            Text("Aria account").font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                }

                SectionTitle(text: "Appearance")
                Card {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Theme").font(.subheadline).foregroundStyle(.secondary)
                        Picker("Theme", selection: $themeMode) {
                            ForEach(ThemeMode.allCases) { Text($0.label).tag($0.rawValue) }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                    }
                }

                Card {
                    Button(role: .destructive) {
                        _Concurrency.Task { await auth.signOut() }
                    } label: {
                        Label("Sign out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(Brand.red)
                }

                Spacer()
            }
            .padding(20)
        }
    }
}
