import SwiftUI

/// Expanded class list for a single weekday. Shows gap markers between
/// sessions and an empty state for free days. Used by `ScheduleFocusedView`.
struct DayColumn: View {
    let classes: [ScheduleClass]
    let isToday: Bool
    let nowMin: Int
    var showGaps: Bool = true
    var entering: Bool = false

    // Easter egg: long-pressing the empty-state mascot on a free day
    // opens the Folio runner. State lives here (not in the parent) so it
    // doesn't leak into days that have classes — the empty state is the
    // only entry point.
    @State private var showFolioRunner = false

    var body: some View {
        if classes.isEmpty {
            emptyState
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 30)
                .padding(.vertical, 60)
                .contentShape(Rectangle())
                // Held distinctly longer than the iOS system long-press
                // (~0.5s) so an accidental hold won't open the easter egg
                // — discovery should feel like a deliberate secret.
                .onLongPressGesture(minimumDuration: 1.2) {
                    showFolioRunner = true
                }
                .sensoryFeedback(.impact(weight: .medium), trigger: showFolioRunner)
                .fullScreenCover(isPresented: $showFolioRunner) {
                    FolioRunnerView()
                }
        } else {
            VStack(spacing: 0) {
                ForEach(Array(classes.enumerated()), id: \.element.id) { i, cls in
                    let prev = i > 0 ? classes[i - 1] : nil
                    let gapMin = prev.map { cls.startMin - $0.endMin } ?? 0
                    let baseDelay = entering ? 0.32 + Double(i) * 0.08 : Double(i) * 0.04
                    let block = FocusedClassBlock(
                        cls: cls,
                        state: stateFor(cls, isToday: isToday, nowMin: nowMin),
                        showGap: showGaps && i > 0,
                        gapMin: gapMin
                    )
                    if let seed = detailSeed(for: cls) {
                        NavigationLink(value: seed) { block }
                            .buttonStyle(.plain)
                            .fadeUpOnAppear(delay: baseDelay, distance: 10, duration: 0.45)
                    } else {
                        block
                            .fadeUpOnAppear(delay: baseDelay, distance: 10, duration: 0.45)
                    }
                }
            }
            .padding(.horizontal, 10)
            .padding(.bottom, 20)
        }
    }

    // Minimal seed for `DisciplineDetailView` — the detail view model swaps
    // this out with a hydrated Discipline as soon as its KMP flow emits, so
    // only offerId and display-chrome fields need to be right here. Without
    // an offerId (fixture / pre-sync), the row renders non-tappable.
    private func detailSeed(for cls: ScheduleClass) -> Discipline? {
        guard let offerId = cls.offerId else { return nil }
        return Discipline(
            code: cls.code,
            fullCode: cls.code,
            title: cls.title,
            dept: "",
            prof: cls.prof,
            color: cls.color,
            hours: 0,
            absences: 0,
            allowedAbsences: 0,
            sections: [],
            offerId: offerId
        )
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
            BlinkingFolio(size: 96)
                .opacity(0.85)
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
