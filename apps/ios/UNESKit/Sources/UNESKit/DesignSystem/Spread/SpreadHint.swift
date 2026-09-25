import SwiftUI

/// The facing page of a list tab while nothing is open on it.
struct SpreadHint: View {
    let systemImage: String
    let text: LocalizedStringResource

    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()

            VStack(spacing: 10) {
                Image(systemName: systemImage)
                    .font(.system(size: 26, weight: .regular))
                    .foregroundStyle(UNESColor.ink4)
                Text(text)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 40)
            .fadeUp(delay: 0.24)
        }
    }
}

#Preview {
    SpreadHint(systemImage: "envelope.open", text: .messagesSpreadHint)
}
