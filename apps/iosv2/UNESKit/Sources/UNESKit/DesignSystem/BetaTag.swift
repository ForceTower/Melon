import SwiftUI

/// Uppercase capsule marking a feature still in beta — coral on a faint
/// coral wash, sitting opposite the icon on hub tiles.
struct BetaTag: View {
    private let tone = UNESColor.readable(0xE85D4E)

    var body: some View {
        Text("Beta")
            .textCase(.uppercase)
            .font(.system(size: 10, weight: .heavy))
            .tracking(0.7)
            .foregroundStyle(tone)
            .padding(.horizontal, 8)
            .padding(.vertical, 4.5)
            .background(tone.opacity(0.11), in: Capsule())
            .overlay {
                Capsule().strokeBorder(tone.opacity(0.28))
            }
    }
}

#Preview {
    BetaTag()
        .padding(28)
        .background(UNESColor.card)
}
