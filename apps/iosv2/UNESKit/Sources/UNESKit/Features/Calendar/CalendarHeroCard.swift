import SwiftUI

/// The next actionable date over a category-tinted mesh: eyebrow with the
/// live dot, category chip, title, display-size countdown and — while a
/// multi-day event runs — its elapsed progress.
struct CalendarHeroCard: View {
    let event: CalendarEvent
    let today: Date
    var onOpen: (CalendarEvent) -> Void

    @State private var progressShown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var category: CalendarCategory { event.category }
    private var isActive: Bool { CalendarMath.status(event, today: today) == .active }
    private var countdown: CalendarCountdown { CalendarMath.countdown(event, today: today) }

    var body: some View {
        Button {
            onOpen(event)
        } label: {
            ZStack {
                Color(hex: 0x160E1F)
                MeshView(variant: category.mesh)
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.scrim.opacity(0.12), location: 0),
                        .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                    ],
                    angle: 160
                )

                VStack(alignment: .leading, spacing: 0) {
                    eyebrowRow
                    categoryChip
                        .padding(.top, 15)
                    Text(event.title)
                        .font(.system(size: 22, weight: .bold))
                        .tracking(-0.66)
                        .lineSpacing(2)
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.leading)
                        .padding(.top, 11)
                    countdownRow
                        .padding(.top, 16)
                    if let progress {
                        progressBar(progress)
                            .padding(.top, 15)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(EdgeInsets(top: 17, leading: 19, bottom: 19, trailing: 19))
            }
            .environment(\.colorScheme, .dark)
            .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
            .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
        }
        .buttonStyle(TilePressStyle())
    }

    private var eyebrowRow: some View {
        HStack {
            HStack(spacing: 7) {
                haloDot
                Text(isActive ? String.localized(.calendarHeroHappeningNow) : String.localized(.calendarHeroNextAction))
                    .font(.system(size: 11.5, weight: .semibold))
                    .tracking(-0.06)
            }
            .foregroundStyle(.white.opacity(0.9))

            Spacer()

            Text(CalendarFormat.dateRange(start: event.start, end: event.end))
                .font(.system(size: 11.5, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(.white.opacity(0.62))
        }
    }

    /// The category dot inside its soft halo ring, pulsing while active.
    private var haloDot: some View {
        Circle()
            .fill(category.color)
            .frame(width: 7, height: 7)
            .background {
                Circle()
                    .fill(category.color.opacity(0.25))
                    .frame(width: 13, height: 13)
            }
            .modifier(CalendarPulse(active: isActive))
    }

    private var categoryChip: some View {
        HStack(spacing: 7) {
            Image(systemName: category.icon)
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 20, height: 20)
                .background(category.color, in: RoundedRectangle(cornerRadius: 6, style: .continuous))
            Text("\(category.label) · \(event.scope.label)")
                .font(.system(size: 11.5, weight: .semibold))
                .tracking(-0.06)
                .foregroundStyle(.white)
        }
        .padding(EdgeInsets(top: 5, leading: 6, bottom: 5, trailing: 9))
        .background(.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 9, style: .continuous))
    }

    private var countdownRow: some View {
        HStack(alignment: .lastTextBaseline, spacing: 10) {
            Text(countdown.number)
                .font(.system(size: countdown.number.count > 3 ? 34 : 52, weight: .bold))
                .tracking(countdown.number.count > 3 ? -1.36 : -2.08)
                .monospacedDigit()
                .foregroundStyle(.white)
            if !countdown.tail.isEmpty {
                Text(countdown.tail)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.82))
            }
        }
    }

    // MARK: Progress

    /// Elapsed share of a running multi-day event, 0…1.
    private var progress: Double? {
        guard isActive, let end = event.end else { return nil }
        let total = CalendarMath.daysBetween(event.start, end) + 1
        let elapsed = CalendarMath.daysBetween(event.start, today) + 1
        guard total > 0 else { return nil }
        return min(1, max(0, Double(elapsed) / Double(total)))
    }

    private func progressBar(_ progress: Double) -> some View {
        VStack(spacing: 7) {
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.16))
                    Capsule()
                        .fill(category.color)
                        .frame(width: geometry.size.width * (progressShown ? progress : 0))
                }
            }
            .frame(height: 4)
            .onAppear {
                guard !progressShown else { return }
                if reduceMotion {
                    progressShown = true
                } else {
                    withAnimation(UNESMotion.settle(0.8).delay(0.35)) {
                        progressShown = true
                    }
                }
            }

            HStack {
                Text(.calendarHeroPercentElapsed(Int((progress * 100).rounded())))
                Spacer()
                if let end = event.end {
                    Text(.calendarHeroEndsOn(CalendarFormat.dateShort(end)))
                }
            }
            .font(.system(size: 11, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(.white.opacity(0.6))
        }
    }
}

/// The design's `cvPulse` keyframes: opacity easing 1 → 0.35 → 1.
struct CalendarPulse: ViewModifier {
    var active: Bool
    var duration: Double = 1.8

    @State private var dimmed = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(dimmed ? 0.35 : 1)
            .onAppear {
                guard active, !reduceMotion else { return }
                withAnimation(.easeInOut(duration: duration / 2).repeatForever(autoreverses: true)) {
                    dimmed = true
                }
            }
    }
}

#Preview {
    let events: [CalendarEvent] = .preview()
    VStack(spacing: 16) {
        CalendarHeroCard(
            event: events.first { $0.end != nil && !$0.closed }!,
            today: .now,
            onOpen: { _ in }
        )
        CalendarHeroCard(
            event: events.first { $0.category == .exam }!,
            today: .now,
            onOpen: { _ in }
        )
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
