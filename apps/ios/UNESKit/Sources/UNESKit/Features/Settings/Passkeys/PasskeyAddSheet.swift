import SwiftUI

/// The create bottom sheet: pick where the passkey lives, then "Continuar"
/// hands off to the system passkey sheet (Face ID / key tap). We never draw
/// that step — we just reflect its outcome as the success state.
struct PasskeyAddSheet: View {
    var accountName: String
    var avatarInitial: String
    var target: PasskeyTarget
    var step: PasskeysFeature.AddStep
    var isCreating: Bool
    var error: String?
    var onSelect: (PasskeyTarget) -> Void
    var onContinue: () -> Void
    var onCancel: () -> Void

    @State private var height: CGFloat = 380

    var body: some View {
        VStack(spacing: 0) {
            accountCard
                .padding(.bottom, 18)

            switch step {
            case .choose:
                chooseStep
            case .success:
                successStep
            }
        }
        .padding(EdgeInsets(top: 22, leading: 20, bottom: 16, trailing: 20))
        .frame(maxWidth: .infinity)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height = $0 }
        .presentationBackground(UNESColor.card)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
        .presentationCornerRadiusCompat(30)
    }

    private var accountCard: some View {
        HStack(spacing: 12) {
            Text(avatarInitial)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 40, height: 40)
                .background(
                    LinearGradient.css(
                        stops: [
                            .init(color: UNESColor.amber, location: 0),
                            .init(color: UNESColor.coral, location: 0.55),
                            .init(color: UNESColor.magenta, location: 1),
                        ],
                        angle: 135
                    ),
                    in: Circle()
                )
            Text(accountName)
                .font(.system(size: 15, weight: .semibold))
                .tracking(-0.15)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Image(systemName: "key.fill")
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(UNESColor.accent)
        }
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 14))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.line)
        }
    }

    private var chooseStep: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.passkeysAddTitle)
                .font(.system(size: 21, weight: .bold))
                .tracking(-0.63)
                .foregroundStyle(UNESColor.ink)
            Text(.passkeysAddMessage)
                .font(.system(size: 13, weight: .medium))
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 4)
                .padding(.bottom, 16)

            VStack(spacing: 9) {
                targetOption(
                    .thisDevice,
                    glyph: "iphone",
                    tint: UNESColor.violet,
                    title: String.localized(.passkeysTargetThisDevice),
                    subtitle: String.localized(.passkeysTargetThisDeviceSub)
                )
                targetOption(
                    .securityKey,
                    glyph: "key.fill",
                    tint: UNESColor.tangerine,
                    title: String.localized(.passkeysTargetSecurityKey),
                    subtitle: String.localized(.passkeysTargetSecurityKeySub)
                )
            }

            if let error {
                Text(error)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.alertRed)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 12)
            }

            continueButton
                .padding(.top, 18)

            Button(action: onCancel) {
                Text(.commonCancel)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
            }
            .padding(.top, 4)
        }
    }

    private func targetOption(
        _ option: PasskeyTarget,
        glyph: String,
        tint: Color,
        title: String,
        subtitle: String
    ) -> some View {
        let selected = target == option
        return Button {
            onSelect(option)
        } label: {
            HStack(spacing: 13) {
                Image(systemName: glyph)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .background(tint, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .shadow(color: tint.opacity(0.3), radius: 8, y: 4)

                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                    Text(subtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                ZStack {
                    Circle()
                        .strokeBorder(selected ? UNESColor.accent : UNESColor.surface3, lineWidth: 2)
                        .frame(width: 21, height: 21)
                    if selected {
                        Circle().fill(UNESColor.accent).frame(width: 21, height: 21)
                        Image(systemName: "checkmark")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white)
                    }
                }
            }
            .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 14))
            .background(
                selected ? UNESColor.accent.opacity(0.12) : UNESColor.surface2,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(selected ? UNESColor.accent : UNESColor.line, lineWidth: 1.5)
            }
        }
        .buttonStyle(TilePressStyle())
    }

    private var continueButton: some View {
        Button(action: onContinue) {
            Group {
                if isCreating {
                    ProgressView().tint(.white)
                } else {
                    Text(.commonContinue)
                        .font(.system(size: 16, weight: .semibold))
                        .tracking(-0.16)
                }
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
            .shadow(color: UNESColor.accent.opacity(0.35), radius: 12, y: 8)
        }
        .buttonStyle(TilePressStyle())
        .disabled(isCreating)
    }

    private var successStep: some View {
        VStack(spacing: 0) {
            Image(systemName: "checkmark")
                .font(.system(size: 36, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 74, height: 74)
                .background(UNESColor.successGreen, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .shadow(color: UNESColor.successGreen.opacity(0.4), radius: 20, y: 10)
                .popIn(from: 0.6, overshoot: 1.4)

            Text(.passkeysAddSuccessTitle)
                .font(.system(size: 19, weight: .bold))
                .tracking(-0.57)
                .foregroundStyle(UNESColor.ink)
                .padding(.top, 16)
            Text(.passkeysAddSuccessSubtitle)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        PasskeyAddSheet(
            accountName: "Mariana Nogueira",
            avatarInitial: "M",
            target: .thisDevice,
            step: .choose,
            isCreating: false,
            error: nil,
            onSelect: { _ in },
            onContinue: {},
            onCancel: {}
        )
    }
}
