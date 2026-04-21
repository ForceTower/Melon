import SwiftUI

/// Expanded class list for a single weekday. Shows gap markers between
/// sessions and an empty state for free days. Used by `ScheduleFocusedView`.
struct DayColumn: View {
    let classes: [ScheduleClass]
    let isToday: Bool
    let nowMin: Int
    var showGaps: Bool = true
    var entering: Bool = false

    var body: some View {
        if classes.isEmpty {
            emptyState
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 30)
                .padding(.vertical, 60)
        } else {
            VStack(spacing: 0) {
                ForEach(Array(classes.enumerated()), id: \.element.id) { i, cls in
                    let prev = i > 0 ? classes[i - 1] : nil
                    let gapMin = prev.map { cls.startMin - $0.endMin } ?? 0
                    let baseDelay = entering ? 0.32 + Double(i) * 0.08 : Double(i) * 0.04
                    FocusedClassBlock(
                        cls: cls,
                        state: stateFor(cls, isToday: isToday, nowMin: nowMin),
                        showGap: showGaps && i > 0,
                        gapMin: gapMin
                    )
                    .fadeUpOnAppear(delay: baseDelay, distance: 10, duration: 0.45)
                }
            }
            .padding(.horizontal, 10)
            .padding(.bottom, 20)
        }
    }

    // Local counterpart to `ScheduleFixtures.state(for:isToday:)` that uses a
    // live `nowMin` instead of a static fixture constant — so the NOW chip
    // flips in real time as the clock advances.
    private func stateFor(_ cls: ScheduleClass, isToday: Bool, nowMin: Int) -> ScheduleClassState {
        guard isToday else { return .future }
        if nowMin >= cls.endMin { return .done }
        if nowMin >= cls.startMin { return .now }
        if cls.startMin - nowMin < 60 { return .next }
        return .later
    }

    private var emptyState: some View {
        VStack(spacing: 6) {
            Text("—")
                .font(UNESFont.serif(42, italic: true))
                .foregroundStyle(UNESColor.ink3)
                .opacity(0.3)
                .padding(.bottom, 6)
            Text("Nenhuma aula")
                .font(UNESFont.serif(20))
                .tracking(-0.2)
                .foregroundStyle(UNESColor.ink3)
            Text("Dia livre. Aproveite.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}
