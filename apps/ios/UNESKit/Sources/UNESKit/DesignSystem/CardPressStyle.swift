import SwiftUI

/// Tappable cards press to 98.5% — the v2 `.t-card:active` behavior.
struct CardPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.985 : 1)
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == CardPressStyle {
    static var pressableCard: CardPressStyle { CardPressStyle() }
}
