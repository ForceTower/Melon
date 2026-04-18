import SwiftUI

struct HomeHeader: View {
    private let now = Date()

    private var greeting: String {
        switch Calendar.current.component(.hour, from: now) {
        case ..<12:  return "Bom dia"
        case ..<18:  return "Boa tarde"
        default:     return "Boa noite"
        }
    }

    private var dateEyebrow: String {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "pt_BR")
        fmt.dateFormat = "EEEE · d MMM"
        let raw = fmt.string(from: now)
        // "quinta-feira" → "quinta", strip trailing period on abbreviated month.
        return raw
            .replacingOccurrences(of: "-feira", with: "")
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    var body: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 6) {
                Text("◦ \(dateEyebrow)")
                    .font(UNESFont.sans(12, weight: .medium))
                    .tracking(1.44)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)

                Text("\(Text("\(greeting), ").foregroundStyle(UNESColor.ink))\(Text("Mariana").italic().foregroundStyle(UNESColor.accent))")
                    .font(UNESFont.serif(30))
                    .tracking(-0.6)
                    .lineSpacing(4.5)
            }

            Spacer()

            HStack(spacing: 8) {
                IconButton {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                }
                IconButton {
                    AvatarDot(initial: "M")
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 60)
        .padding(.bottom, 26)
    }
}

private struct IconButton<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .frame(width: 40, height: 40)
            .background(
                Circle()
                    .fill(UNESColor.card.opacity(0.72))
                    .overlay(Circle().strokeBorder(UNESColor.cardLine, lineWidth: 1))
            )
    }
}

private struct AvatarDot: View {
    let initial: String
    var body: some View {
        Text(initial)
            .font(UNESFont.serif(15))
            .foregroundStyle(UNESColor.surfaceLight)
            .frame(width: 32, height: 32)
            .background(
                Circle().fill(
                    LinearGradient(
                        colors: [UNESColor.coral, UNESColor.amber],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            )
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        VStack { HomeHeader(); Spacer() }
    }
}
