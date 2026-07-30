import SwiftUI

/// Status tones for inline callouts — dark-readable like the discipline palette.
enum UNESBannerTone {
    static let danger = UNESColor.readable(0xE85D4E)
    static let warn = UNESColor.readable(0xD9852E)
    static let ok = UNESColor.readable(0x2F9E5E)
}

/// Tonal inline callout: icon disc, optional title, free-form body and an
/// optional inline action. Used for the matrícula warnings and the
/// session-expired notice on Hoje.
struct UNESBanner<Content: View>: View {
    enum Tone {
        case danger, warn, info, neutral
    }

    var tone: Tone
    var title: String?
    var action: String?
    var onAction: (() -> Void)?
    @ViewBuilder var content: Content

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 10.5, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 22, height: 22)
                .background(color, in: Circle())
                .padding(.top, 1)

            VStack(alignment: .leading, spacing: 2) {
                if let title {
                    Text(title)
                        .font(.system(size: 13.5, weight: .semibold))
                        .tracking(-0.13)
                        .foregroundStyle(UNESColor.ink)
                }
                content
                    .font(.system(size: 12.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let action {
                    Button {
                        onAction?()
                    } label: {
                        Text(verbatim: "\(action) →")
                            .font(.system(size: 12.5, weight: .semibold))
                            .foregroundStyle(color)
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 5)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .background(fill, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(border)
        }
    }

    private var color: Color {
        switch tone {
        case .danger: UNESBannerTone.danger
        case .warn: UNESBannerTone.warn
        case .info: UNESBannerTone.ok
        case .neutral: UNESColor.ink3
        }
    }

    private var icon: String {
        switch tone {
        case .danger, .warn: "exclamationmark.triangle.fill"
        case .info: "checkmark"
        case .neutral: "info"
        }
    }

    private var fill: Color {
        tone == .neutral ? UNESColor.surface2 : color.opacity(0.12)
    }

    private var border: Color {
        tone == .neutral ? UNESColor.line : color.opacity(0.26)
    }
}
