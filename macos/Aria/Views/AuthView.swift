import SwiftUI

struct AuthView: View {
    @EnvironmentObject var auth: AuthModel
    @State private var isSignUp = false
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: "sparkles")
                .font(.system(size: 40))
                .foregroundStyle(.white)
                .frame(width: 72, height: 72)
                .background(RoundedRectangle(cornerRadius: 18).fill(Brand.indigo))
            Text(isSignUp ? "Create your account" : "Welcome to Aria").font(.title.weight(.bold))
            Text("Sync your day across your devices").foregroundStyle(.secondary)

            if !Config.isConfigured {
                Text("Set your Supabase URL and anon key in Config.swift to enable accounts.")
                    .font(.footnote).foregroundStyle(Brand.amber).multilineTextAlignment(.center)
            }

            VStack(spacing: 12) {
                if isSignUp {
                    TextField("Name", text: $name).textFieldStyle(.roundedBorder)
                }
                TextField("Email", text: $email).textFieldStyle(.roundedBorder)
                SecureField("Password", text: $password).textFieldStyle(.roundedBorder)
                if let err = auth.error {
                    Text(err).font(.footnote).foregroundStyle(Brand.red)
                }
                Button {
                    Foundation.Task {
                        if isSignUp { await auth.signUp(email, password, name: name) }
                        else { await auth.signIn(email, password) }
                    }
                } label: {
                    Text(isSignUp ? "Sign up" : "Log in").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent).tint(Brand.indigo).controlSize(.large)
                .disabled(auth.busy || email.isEmpty || password.isEmpty)
            }
            .frame(width: 320)

            Button(isSignUp ? "Already have an account? Log in" : "New here? Create an account") {
                auth.error = nil; isSignUp.toggle()
            }.buttonStyle(.link)
        }
        .padding(40)
        .frame(minWidth: 460, minHeight: 520)
    }
}
