import SwiftUI

// UNES — inline notice banner (conflict / warning / confirmation). Ported from
// `MatBanner` in `screens-matricula-ui.jsx`.
struct EnrollmentBanner<Content: View>: View {
    enum Tone { case danger, warn, info }

    let tone: Tone
    var title: String?
    var systemImage: String?
    @ViewBuilder var content: () -> Content

    init(
        tone: Tone,
        title: String? = nil,
        systemImage: String? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.tone = tone
        self.title = title
        self.systemImage = systemImage
        self.content = content
    }

    private var accent: Color {
        switch tone {
        case .danger: return EnrollmentPalette.danger
        case .warn:   return EnrollmentPalette.warn
        case .info:   return EnrollmentPalette.ok
        }
    }

    private var icon: String {
        if let systemImage { return systemImage }
        return tone == .info ? "checkmark" : "exclamationmark"
    }

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 22, height: 22)
                .background(Circle().fill(accent))

            VStack(alignment: .leading, spacing: 2) {
                if let title {
                    Text(title)
                        .font(UNESFont.sans(13, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                }
                content()
                    .font(UNESFont.sans(12))
                    .foregroundStyle(UNESColor.ink2)
                    .lineSpacing(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(accent.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(accent.opacity(0.20), lineWidth: 1)
                )
        )
    }
}
