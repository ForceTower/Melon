import SwiftUI

// UNES — enrollment routes + load placeholders. The whole matrícula flow
// value-routes on the Me hub's `NavigationStack` (each screen is a pushed
// `EnrollmentRoute`), so every transition gets the native back chevron and the
// entry screen returns to Me. The screens are built by
// `MeView.enrollmentDestination(for:)`, mirroring `OnboardingFlow.destination`.

enum EnrollmentRoute: Hashable {
    case window
    case offers
    case picker(Int64)      // discipline id, resolved from the loaded offers
    case timetable
    case review
    case success
}

// Shown while the live window/offers load.
struct EnrollmentLoadingView: View {
    var body: some View {
        VStack(spacing: 12) {
            SpinnerView(color: UNESColor.ink3).frame(width: 22, height: 22)
            Text("Carregando matrícula…")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(UNESColor.surface.ignoresSafeArea())
        .navigationTitle("Matrícula")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// Shown when the live load fails, with a retry.
struct EnrollmentLoadFailedView: View {
    let message: String
    let retry: () async -> Void

    @State private var retrying = false

    var body: some View {
        VStack(spacing: 14) {
            Text("◦").font(UNESFont.serif(34)).foregroundStyle(UNESColor.ink4)
            Text(message)
                .font(UNESFont.sans(14))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button {
                Task { retrying = true; await retry(); retrying = false }
            } label: {
                Text(retrying ? "Tentando…" : "Tentar de novo")
                    .font(UNESFont.sans(13.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
            }
            .disabled(retrying)
        }
        .padding(28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(UNESColor.surface.ignoresSafeArea())
        .navigationTitle("Matrícula")
        .navigationBarTitleDisplayMode(.inline)
    }
}
