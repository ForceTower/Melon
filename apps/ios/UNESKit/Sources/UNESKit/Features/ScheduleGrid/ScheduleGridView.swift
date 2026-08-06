import ComposableArchitecture
import SwiftUI

/// The week-grid Horário: the whole week as one proportional time grid with
/// the agenda list beneath it. Swapped with `ScheduleView` by a settings
/// flag at runtime.
struct ScheduleGridView: View {
    @Bindable var store: StoreOf<ScheduleGridFeature>

    var body: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
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

    private func loaded(_ overview: ScheduleOverview) -> some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let calendar = Calendar.current
            let nowMinutes = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
            let todayIndex = overview.todayIndex(now: now)
            let layout = ScheduleGridLayout(days: overview.days)
            let weekIsEmpty = layout.dayIndices.allSatisfy { overview.days[$0].classes.isEmpty }

            ScrollView {
                VStack(spacing: 0) {
                    eyebrow(overview, todayIndex: todayIndex)
                        .slideIn(delay: 0.02)

                    ScheduleGridDayHeader(days: overview.days, layout: layout, todayIndex: todayIndex)
                        .fadeIn(delay: 0.02)

                    ScheduleGridWeekCanvas(
                        days: overview.days,
                        layout: layout,
                        todayIndex: todayIndex,
                        nowMinutes: nowMinutes
                    ) { scheduleClass, dayIndex in
                        store.send(.classTapped(scheduleClass, dayIndex: dayIndex))
                    }
                    .padding(.bottom, 28)

                    if weekIsEmpty {
                        Text(.scheduleGridEmptyWeek)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(UNESColor.ink3)
                            .padding(EdgeInsets(top: 0, leading: 20, bottom: 24, trailing: 20))
                            .fadeUp(delay: 0.2)
                    } else {
                        ScheduleGridAgendaList(
                            days: overview.days,
                            layout: layout,
                            todayIndex: todayIndex,
                            nowMinutes: nowMinutes
                        ) { scheduleClass, dayIndex in
                            store.send(.classTapped(scheduleClass, dayIndex: dayIndex))
                        }
                    }
                }
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .refreshable {
                await store.send(.refreshPulled).finish()
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
