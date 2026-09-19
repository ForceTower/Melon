import ComposableArchitecture
import SwiftUI

/// The week-grid Horário: the whole week as one proportional time grid with
/// the agenda list beneath it — or facing it, on a spread. Swapped with
/// `ScheduleView` by a settings flag at runtime.
struct ScheduleGridView: View {
    @Bindable var store: StoreOf<ScheduleGridFeature>
    @Environment(\.pageLayout) private var scenePageLayout
    @Environment(\.deviceFold) private var fold

    /// Flat, the week reads best as one full-width page however wide the
    /// scene is; only a bent spine splits it into grid and agenda.
    private var pageLayout: PageLayout {
        scenePageLayout == .spread && fold?.isBook == true ? .spread : .stack
    }

    var body: some View {
        SpreadStack(path: $store.scope(state: \.path, action: \.path)) {
            ZStack {
                UNESColor.surface.ignoresSafeArea()

                if let overview = store.overview {
                    if overview.days.isEmpty {
                        emptyState
                    } else {
                        loaded(overview)
                    }
                } else if let message = store.errorMessage {
                    errorState(message)
                } else {
                    SpinnerRing(size: 28, color: UNESColor.accent, trackColor: UNESColor.surface3)
                        .frame(maxHeight: .infinity)
                }
            }
            .navigationTitle(Text(.navSchedule))
            .sheet(item: sheetBinding) { item in
                ScheduleGridClassSheet(
                    item: item,
                    onViewDiscipline: { store.send(.sheetDisciplineTapped) },
                    onClose: { store.send(.sheetDismissed) }
                )
            }
        } overview: {
            agendaPage
        } destination: { store in
            switch store.case {
            case let .detail(store):
                DisciplineDetailView(store: store)
            case let .materialsList(store):
                MaterialsListView(store: store)
            case let .materialsDetail(store):
                MaterialsDetailView(store: store)
            }
        }
        .environment(\.pageLayout, pageLayout)
        .task { await store.send(.task).finish() }
    }

    /// Dismissal comes back through the reducer; the completion re-write of
    /// nil after a programmatic dismissal must not re-send.
    private var sheetBinding: Binding<ScheduleGridFeature.SheetItem?> {
        Binding(
            get: { store.sheet },
            set: { value in
                if value == nil, store.sheet != nil { store.send(.sheetDismissed) }
            }
        )
    }

    // MARK: Content

    /// The accent week-and-today line under the system large title — the
    /// same eyebrow treatment as the day-list Horário.
    private func eyebrow(_ overview: ScheduleOverview, todayIndex: Int?) -> some View {
        let label: String = if let todayIndex {
            .localized(.scheduleWeekNumberRange(
                overview.weekOfYear,
                ScheduleGridFormat.todaySummary(count: overview.days[todayIndex].classes.count)
            ))
        } else {
            .localized(.scheduleWeekNumber(overview.weekOfYear))
        }
        return Text(label)
            .textCase(.uppercase)
            .font(.system(size: 13, weight: .semibold))
            .tracking(0.2)
            .monospacedDigit()
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 2, leading: 20, bottom: 4, trailing: 20))
    }

    /// What both pages derive from the week and the current minute.
    private struct Week {
        let overview: ScheduleOverview
        let layout: ScheduleGridLayout
        let todayIndex: Int?
        let nowMinutes: Int

        init(_ overview: ScheduleOverview, now: Date) {
            let calendar = Calendar.current
            self.overview = overview
            layout = ScheduleGridLayout(days: overview.days)
            todayIndex = overview.todayIndex(now: now)
            nowMinutes = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
        }

        var isEmpty: Bool {
            layout.dayIndices.allSatisfy { overview.days[$0].classes.isEmpty }
        }
    }

    private func loaded(_ overview: ScheduleOverview) -> some View {
        weekPage(overview) { week in
            eyebrow(overview, todayIndex: week.todayIndex)
                .slideIn(delay: 0.02)

            ScheduleGridDayHeader(days: overview.days, layout: week.layout, todayIndex: week.todayIndex)
                .fadeIn(delay: 0.02)

            ScheduleGridWeekCanvas(
                days: overview.days,
                layout: week.layout,
                todayIndex: week.todayIndex,
                nowMinutes: week.nowMinutes
            ) { scheduleClass, dayIndex in
                store.send(.classTapped(scheduleClass, dayIndex: dayIndex))
            }
            .padding(.bottom, 28)

            if pageLayout == .stack {
                agenda(week)
            }
        }
    }

    private var agendaPage: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()

            if let overview = store.overview, !overview.days.isEmpty {
                weekPage(overview) { week in
                    agenda(week)
                        .padding(.top, 8)
                }
            }
        }
    }

    private func weekPage(
        _ overview: ScheduleOverview,
        @ViewBuilder content: @escaping (Week) -> some View
    ) -> some View {
        TimelineView(.everyMinute) { context in
            ScrollView {
                VStack(spacing: 0) {
                    content(Week(overview, now: context.date))
                }
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .refreshable {
                await store.send(.refreshPulled).finish()
            }
        }
    }

    @ViewBuilder
    private func agenda(_ week: Week) -> some View {
        if week.isEmpty {
            Text(.scheduleGridEmptyWeek)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(UNESColor.ink3)
                .padding(EdgeInsets(top: 0, leading: 20, bottom: 24, trailing: 20))
                .fadeUp(delay: 0.2)
        } else {
            ScheduleGridAgendaList(
                days: week.overview.days,
                layout: week.layout,
                todayIndex: week.todayIndex,
                nowMinutes: week.nowMinutes
            ) { scheduleClass, dayIndex in
                store.send(.classTapped(scheduleClass, dayIndex: dayIndex))
            }
        }
    }

    // MARK: States

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text(.scheduleEmptyStateTitle)
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(.scheduleEmptyStateMessage)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 8) {
            Text(.scheduleErrorTitle)
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button {
                store.send(.refreshPulled)
            } label: {
                Text(.commonTryAgain)
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(UNESColor.accent)
            .padding(.top, 8)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }
}

#Preview {
    ScheduleGridView(
        store: Store(initialState: ScheduleGridFeature.State()) {
            ScheduleGridFeature()
        }
    )
}
