import SwiftUI

/// Subtle destructive action. Not a full `PrimaryButton` — the prototype
/// treats sign-out as an afterthought at the bottom of the list, so it reads
/// as a pill with a thin border rather than a filled call-to-action.
struct SignOutButton: View {
    var action: () -> Void = {}

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 12, weight: .semibold))
                Text("Sair da conta")
                    .font(UNESFont.sans(13, weight: .medium))
                    .tracking(-0.07)
            }
            .foregroundStyle(MeColors.signOut)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13)
            // Liquid Glass paints over a clear-filled shape which doesn't
            // hit-test — without this the button would only be tappable
            // on the icon + text. Anchor the press target to the pill.
            .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .background {
                if #available(iOS 26.0, *) {
                    // Liquid Glass with a whisper of the destructive tint —
                    // enough to read as a dedicated action, not so much that
                    // it competes with real CTAs higher up the screen.
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color.clear)
                        .glassEffect(
                            .regular.tint(MeColors.signOut.opacity(0.08)),
                            in: RoundedRectangle(cornerRadius: 18, style: .continuous)
                        )
                }
            }
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(UNESColor.line, lineWidth: 1)
            )
        }
        .buttonStyle(PressScaleStyle())
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        SignOutButton()
            .padding()
    }
}
