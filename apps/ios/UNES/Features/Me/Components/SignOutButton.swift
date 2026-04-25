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
            .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .background {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(MeColors.signOut.opacity(0.08))
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
