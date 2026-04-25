import SwiftUI

/// Body of the "Sobre o aplicativo" bottom sheet. Hosted by `MeView` through a
/// native `.sheet(...)` presentation — the scrim, blur, drag indicator, and
/// dismissal gesture are provided by SwiftUI. Mirrors the `AboutSheet`
/// component in `screens-me.jsx`.
struct AboutSheet: View {
    let info: AppInfo
    /// Parent-owned measured height so the sheet's detent can track content.
    /// Same pattern as `LogoutConfirmationSheet`.
    var measuredHeight: Binding<CGFloat>? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var copied: Bool = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, 18)

            wordmarkHero
                .padding(.bottom, 12)

            detailRows
                .padding(.bottom, 16)

            copyButton
                .padding(.bottom, 12)

            footer
        }
        .padding(.horizontal, 18)
        .padding(.top, 20)
        .padding(.bottom, 22)
        .background(
            GeometryReader { proxy in
                Color.clear
                    .preference(key: AboutSheetHeightKey.self, value: proxy.size.height)
            }
        )
        .onPreferenceChange(AboutSheetHeightKey.self) { height in
            measuredHeight?.wrappedValue = height
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            iconTile

            VStack(alignment: .leading, spacing: 3) {
                Text("Sobre o aplicativo")
                    .font(UNESFont.serif(22))
                    .tracking(-0.33)
                    .foregroundStyle(UNESColor.ink)

                Text("UNES · INFORMAÇÕES DE DEPURAÇÃO")
                    .font(UNESFont.mono(10))
                    .tracking(0.8)
                    .foregroundStyle(UNESColor.ink3)
            }

            Spacer(minLength: 0)

            closeButton
        }
    }

    private var iconTile: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(UNESColor.plum)
            Image(systemName: "info.circle")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(UNESColor.peach)
        }
        .frame(width: 40, height: 40)
        .shadow(color: UNESColor.plum.opacity(0.35), radius: 14, x: 0, y: 6)
    }

    private var closeButton: some View {
        Button(action: { dismiss() }) {
            Image(systemName: "xmark")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
                .frame(width: 30, height: 30)
                .background(Circle().fill(UNESColor.surface2))
        }
        .buttonStyle(.plain)
    }

    private var wordmarkHero: some View {
        HStack(alignment: .firstTextBaseline) {
            Text("UNES")
                .font(UNESFont.serif(44, italic: true))
                .tracking(-1.32)
                .foregroundStyle(UNESColor.accent)

            Spacer(minLength: 0)

            VStack(alignment: .trailing, spacing: 3) {
                Text(info.version)
                    .font(UNESFont.serif(24))
                    .tracking(-0.48)
                    .foregroundStyle(UNESColor.ink)

                Text("BUILD \(info.build)")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.95)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity)
        .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var detailRows: some View {
        VStack(spacing: 0) {
            detailRow(label: "VERSÃO",      value: info.version,       sub: info.channel,                       valueIsMono: false)
            divider
            detailRow(label: "BUILD",       value: info.build,         sub: "origem · \(info.installSource)",   valueIsMono: false)
            divider
            detailRow(label: "MACHINE ID",  value: info.machineId,     sub: "gerado para depuração",            valueIsMono: true)
            divider
            detailRow(label: "APARELHO",    value: info.phoneModel,    sub: "detectado automaticamente",        valueIsMono: false)
        }
        .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private var divider: some View {
        Rectangle()
            .fill(UNESColor.line)
            .frame(height: 1)
    }

    private func detailRow(label: String, value: String, sub: String, valueIsMono: Bool) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(UNESFont.mono(9))
                .tracking(1.26)
                .foregroundStyle(UNESColor.ink4)

            Text(value)
                .font(valueIsMono ? UNESFont.mono(12.5, weight: .medium) : UNESFont.sans(14, weight: .semibold))
                .tracking(valueIsMono ? 0.25 : -0.07)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)

            Text(sub)
                .font(UNESFont.mono(9.5))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private var copyButton: some View {
        Button(action: copy) {
            HStack(spacing: 8) {
                Image(systemName: copied ? "checkmark" : "doc.on.doc")
                    .font(.system(size: 12, weight: .semibold))
                Text(copied ? "Copiado para a área de transferência" : "Copiar informações")
                    .font(UNESFont.sans(13.5, weight: .semibold))
                    .tracking(-0.07)
            }
            .foregroundStyle(copied ? UNESColor.surfaceLight : UNESColor.surface)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(copied ? MeColors.okGreen : UNESColor.ink)
            )
            .shadow(
                color: copied ? MeColors.okGreen.opacity(0.35) : Color.black.opacity(0.20),
                radius: 12,
                x: 0,
                y: copied ? 10 : 12
            )
        }
        .buttonStyle(PressScaleStyle())
        .animation(.easeOut(duration: 0.2), value: copied)
    }

    private var footer: some View {
        Text("◦ FEITO COM ♥ EM FEIRA DE SANTANA ◦")
            .font(UNESFont.mono(9))
            .tracking(1.26)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
    }

    private func copy() {
        UIPasteboard.general.string = info.debugText
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        copied = true
        Task { @MainActor in
            try? await Task.sleep(nanoseconds: 1_800_000_000)
            copied = false
        }
    }
}

private struct AboutSheetHeightKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = max(value, nextValue())
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        AboutSheet(info: AppInfo.current)
    }
}
