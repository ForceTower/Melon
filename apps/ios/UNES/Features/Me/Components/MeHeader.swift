import SwiftUI

/// Top chrome of the Me screen. Mirrors the inline-header pattern used by
/// `OverviewHeader` / `MessagesListView.header`: eyebrow + serif greeting on
/// the left, a single circular action button on the right. Kept in the
/// scroll view (not the native nav bar) so the ambient mesh can show through.
struct MeHeader: View {
    let identity: ProfileIdentity
    var onSettings: () -> Void = {}

    var body: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 6) {
                Text("◦ PERFIL · \(identity.semester)")
                    .font(UNESFont.sans(12, weight: .medium))
                    .tracking(1.44)
                    .foregroundStyle(UNESColor.ink3)

                Text("\(Text("Olá, ").foregroundStyle(UNESColor.ink))\(Text(identity.firstName).italic().foregroundStyle(UNESColor.accent))")
                    .font(UNESFont.serif(32))
                    .tracking(-0.64)
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 18)
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        VStack {
            MeHeader(identity: MeFixtures.identity)
            Spacer()
        }
    }
}
