import SwiftUI

/// The "Sobre o aplicativo" bottom sheet: identity hero, the technical-info
/// grid, and the copy-debug-info button.
struct MeAboutSheet: View {
    var info: AppInfo
    var isCopied: Bool
    var onCopy: () -> Void
    var onClose: () -> Void

    /// Measured content height so the sheet hugs it instead of a fixed detent.
    /// Same pattern as `MeLogoutSheet`.
    @State private var height: CGFloat = 480

    private static let infoBlue = UNESColor.readable(0x0A84FF)
    /// The design's `#160E1F` backdrop, matching the identity hero.
    private static let backdrop = Color(hex: 0x160E1F)

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            hero
                .padding(.top, 18)
            techGrid
                .padding(.top, 12)
            copyButton
                .padding(.top, 14)
            footer
                .padding(.top, 13)
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 12, trailing: 18))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle")
                .font(.system(size: 21, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(Self.infoBlue, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .shadow(color: Self.infoBlue.opacity(0.33), radius: 7, y: 6)

            VStack(alignment: .leading, spacing: 2) {
                Text("Sobre o aplicativo")
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                Text("versão, build e depuração")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: Identity + version hero

    private var hero: some View {
        ZStack {
            Self.backdrop
            MeshView(variant: .rose, intensity: 0.9)
            LinearGradient.css(
                stops: [
                    .init(color: Color(hex: 0x0A0810, opacity: 0.15), location: 0),
                    .init(color: Color(hex: 0x0A0810, opacity: 0.62), location: 1),
                ],
                angle: 155
            )

            HStack(spacing: 13) {
                iconTile
                VStack(alignment: .leading, spacing: 4) {
                    Text("UNES")
                        .font(.system(size: 23, weight: .bold))
                        .tracking(-0.92)
                        .foregroundStyle(.white)
                    Text("o semestre da UEFS no bolso")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.64))
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Text(info.version)
                    .font(.system(size: 24, weight: .bold))
                    .tracking(-0.72)
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            .padding(EdgeInsets(top: 16, leading: 16, bottom: 15, trailing: 16))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 16, y: 14)
    }

    private var iconTile: some View {
        Text("U")
            .font(.system(size: 28, weight: .heavy))
            .foregroundStyle(.white)
            .shadow(color: .black.opacity(0.2), radius: 1, y: 1)
            .frame(width: 52, height: 52)
            .background(
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.amber, location: 0),
                        .init(color: UNESColor.coral, location: 0.52),
                        .init(color: UNESColor.magenta, location: 1),
                    ],
                    angle: 145
                ),
                in: RoundedRectangle(cornerRadius: 14, style: .continuous)
            )
            .shadow(color: UNESColor.coral.opacity(0.42), radius: 10, y: 8)
    }

    // MARK: Technical info grid

    private var techGrid: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                cell(label: "Build", value: info.build, sub: "origem · \(info.installSource)")
                cell(label: "Canal", value: info.channel, sub: "versão \(info.version)")
            }
            HStack(spacing: 8) {
                cell(label: "Aparelho", value: info.deviceModel, sub: info.osVersion)
                cell(label: "Machine ID", value: truncatedMachineId, sub: "para depuração", mono: true)
            }
        }
    }

    private func cell(label: String, value: String, sub: String, mono: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.bottom, 4)
            Text(value)
                .font(mono
                    ? .system(size: 13, weight: .semibold, design: .monospaced)
                    : .system(size: 16, weight: .bold))
                .tracking(mono ? 0.13 : -0.32)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .padding(.bottom, 3)
            Text(sub)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var truncatedMachineId: String {
        info.machineId.count > 9 ? "\(info.machineId.prefix(9))…" : info.machineId
    }

    // MARK: Copy + footer

    private var copyButton: some View {
        Button(action: onCopy) {
            HStack(spacing: 8) {
                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                    .font(.system(size: 13, weight: .semibold))
                Text(isCopied ? "Copiado para a área de transferência" : "Copiar informações de depuração")
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(isCopied ? .white : UNESColor.surface)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                isCopied ? UNESColor.successGreen : UNESColor.ink,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .shadow(
                color: isCopied ? UNESColor.successGreen.opacity(0.35) : .black.opacity(0.2),
                radius: 12,
                y: 10
            )
        }
        .buttonStyle(TilePressStyle())
        .animation(.easeOut(duration: 0.2), value: isCopied)
    }

    private var footer: some View {
        Text("feito com ♥ em Feira de Santana")
            .font(.system(size: 11, weight: .medium))
            .tracking(0.2)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeAboutSheet(info: .preview, isCopied: false, onCopy: {}, onClose: {})
    }
}
