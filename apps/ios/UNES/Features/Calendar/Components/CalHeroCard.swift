import SwiftUI

/// "Hero" card at the top of the calendar — the next actionable deadline,
/// rendered over a category-tinted mesh with a giant countdown number and an
/// optional progress bar for active windows.
///
/// Mirrors `CalHero` in `screens-calendar.jsx`.
struct CalHeroCard: View {
    let event: CalendarEvent

    private var category: CalendarCategory { CalendarMath.categorize(event) }
    private var status: CalendarStatus { CalendarMath.status(event) }
    private var isActive: Bool { status == .active }
    private var meshVariant: MeshVariant {
        switch category {
        case .holiday:  return .warm
        case .exam:     return .cool
        case .deadline: return .rose
        }
    }

    private var progress: Double? {
        guard isActive, let end = event.end else { return nil }
        let total = max(1, CalendarMath.daysBetween(event.start, end) + 1)
        let elapsed = CalendarMath.daysBetween(event.start, CalendarMath.today) + 1
        return min(1, max(0, Double(elapsed) / Double(total)))
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            backdrop

            VStack(alignment: .leading, spacing: 0) {
                topRow
                    .padding(.bottom, 16)
                eyebrow
                    .padding(.bottom, 10)
                title
                countdown
                    .padding(.top, 18)
                if let progress {
                    progressBar(progress: progress)
                        .padding(.top, 14)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 18)
            .padding(.bottom, 20)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color.black.opacity(0.14), radius: 18, x: 0, y: 14)
    }

    // MARK: - Sections

    private var backdrop: some View {
        ZStack {
            UNESColor.heroDarkBg
            MeshGradientView(variant: meshVariant, intensity: 1.05)
            LinearGradient(
                stops: [
                    .init(color: UNESColor.heroDarkBg.opacity(0.12), location: 0),
                    .init(color: UNESColor.heroDarkBg.opacity(0.55), location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }

    private var topRow: some View {
        HStack(alignment: .top) {
            HStack(spacing: 6) {
                Circle()
                    .fill(category.color)
                    .frame(width: 6, height: 6)
                    .background(
                        Circle()
                            .fill(category.color.opacity(0.2))
                            .frame(width: 12, height: 12)
                    )
                    .modifier(ConditionalPulse(active: isActive))

                Text(isActive ? "ACONTECENDO AGORA" : "PRÓXIMA AÇÃO")
                    .font(UNESFont.mono(10))
                    .tracking(1.8)
                    .foregroundStyle(UNESColor.heroLight.opacity(0.75))
            }

            Spacer()

            Text(CalendarFormat.dateRange(start: event.start, end: event.end).uppercased())
                .font(UNESFont.mono(10))
                .tracking(0.8)
                .foregroundStyle(UNESColor.heroLight.opacity(0.6))
        }
    }

    private var eyebrow: some View {
        HStack(spacing: 8) {
            CalCategoryGlyph(category: category, color: category.color, size: 13)
            Text("\(category.label.uppercased()) · \(event.scope.label.uppercased())")
                .font(UNESFont.serif(14))
                .tracking(1.68)
                .foregroundStyle(category.color)
        }
    }

    private var title: some View {
        Text(event.displayDescription)
            .font(UNESFont.serif(26))
            .tracking(-0.39)
            .lineSpacing(2)
            .foregroundStyle(UNESColor.heroLight)
            .fixedSize(horizontal: false, vertical: true)
    }

    private var countdown: some View {
        let parts = CalendarMath.countdownParts(event)
        return HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(parts.number)
                .font(UNESFont.serif(48))
                .tracking(-1.44)
                .foregroundStyle(UNESColor.heroLight)
            if !parts.tail.isEmpty {
                Text(parts.tail)
                    .font(UNESFont.serif(16, italic: true))
                    .foregroundStyle(UNESColor.heroLight.opacity(0.8))
                    .padding(.bottom, 6)
            }
        }
    }

    private func progressBar(progress: Double) -> some View {
        VStack(spacing: 6) {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(UNESColor.heroLight.opacity(0.15))
                    Capsule()
                        .fill(category.color)
                        .frame(width: geo.size.width * progress)
                }
            }
            .frame(height: 3)

            HStack {
                Text("\(Int(round(progress * 100)))% DECORRIDO")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.95)
                    .foregroundStyle(UNESColor.heroLight.opacity(0.55))

                Spacer()

                if let end = event.end {
                    Text("FECHA \(CalendarFormat.dateShort(end).uppercased())")
                        .font(UNESFont.mono(9.5))
                        .tracking(0.95)
                        .foregroundStyle(UNESColor.heroLight.opacity(0.55))
                }
            }
        }
    }
}

private struct ConditionalPulse: ViewModifier {
    let active: Bool

    func body(content: Content) -> some View {
        if active { content.pulseForever() } else { content }
    }
}

private extension UNESColor {
    /// Always-dark hero canvas — matches the `#1A0F28` background the JSX
    /// renders behind the mesh, and never flips with the system appearance.
    static let heroDarkBg = Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
    /// Cream type used over the dark hero. Pinned (does not flip in dark mode).
    static let heroLight  = Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
}
