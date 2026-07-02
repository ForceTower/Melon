import ComposableArchitecture
import SwiftUI

struct ScheduleView: View {
    @Bindable var store: StoreOf<ScheduleFeature>

    var body: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
            ZStack(alignment: .top) {
                UNESColor.surface.ignoresSafeArea()
                ambientWash

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
            .navigationTitle("Horário")
        } destination: { store in
            switch store.case {
            case let .detail(store):
                DisciplineDetailView(store: store)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Content

    private func loaded(_ overview: ScheduleOverview) -> some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let calendar = Calendar.current
            let nowMinutes = calendar.component(.hour, from: now) * 60 + calendar.component(.minute, from: now)
            let todayIndex = overview.todayIndex(now: now)
            let selected = min(store.selectedIndex ?? todayIndex ?? 0, overview.days.count - 1)
            let day = overview.days[selected]
            let isToday = selected == todayIndex

            ScrollView {
                VStack(spacing: 0) {
                    eyebrow(overview)
                        .slideIn(delay: 0.02)

                    ScheduleWeekStrip(days: overview.days, selectedIndex: selected, todayIndex: todayIndex) {
                        store.send(.daySelected($0))
                    }
                    .slideIn(delay: 0.1)
                    .padding(EdgeInsets(top: 0, leading: 12, bottom: 4, trailing: 12))

                    dayHeader(day, index: selected, isToday: isToday)
                        .slideIn(delay: 0.16)

                    VStack(spacing: 0) {
                        if isToday, !day.classes.isEmpty {
                            ScheduleStatusHero(classes: day.classes, now: now)
                                .slideIn(delay: 0.2)
                                .padding(EdgeInsets(top: 0, leading: 16, bottom: 20, trailing: 16))
                        }

                        ZStack {
                            ScheduleDayTimeline(day: day, isToday: isToday, nowMinutes: nowMinutes) {
                                store.send(.classTapped($0))
                            }
                            .id(day.id)
                            .transition(.asymmetric(
                                insertion: .offset(x: 8).combined(with: .opacity),
                                removal: .opacity
                            ))
                        }
                        .animation(UNESMotion.ease(0.3), value: day.id)
                        .padding(EdgeInsets(top: 0, leading: 12, bottom: 20, trailing: 16))
                    }
                }
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
            .refreshable {
                await store.send(.refreshPulled).finish()
            }
            .toolbar {
                ToolbarItem(placement: .trailingCompat) {
                    if !isToday {
                        todayButton
                    }
                }
            }
        }
    }

    /// The accent week line under the system large title.
    private func eyebrow(_ overview: ScheduleOverview) -> some View {
        Text(weekLabel(overview))
            .textCase(.uppercase)
            .font(.system(size: 13, weight: .semibold))
            .tracking(0.2)
            .foregroundStyle(UNESColor.accent)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 2, leading: 20, bottom: 10, trailing: 20))
    }

    private func weekLabel(_ overview: ScheduleOverview) -> String {
        guard let first = overview.days.first, let last = overview.days.last else { return "" }
        return ScheduleFormat.weekEyebrow(
            weekOfYear: overview.weekOfYear,
            first: first.dayStamp,
            last: last.dayStamp
        )
    }

    private func dayHeader(_ day: ScheduleDay, index: Int, isToday: Bool) -> some View {
        HStack(alignment: .lastTextBaseline) {
            Text(isToday ? "Hoje" : ScheduleFormat.dayNames[index])
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)

            Spacer()

            Text(ScheduleFormat.daySummary(for: day.classes))
                .font(.system(size: 14, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
        }
        .padding(EdgeInsets(top: 14, leading: 20, bottom: 12, trailing: 20))
    }

    /// Dark pill jumping the strip back to today.
    private var todayButton: some View {
        Button {
            store.send(.todayTapped)
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "smallcircle.filled.circle")
                    .font(.system(size: 11, weight: .semibold))
                Text("Hoje")
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
            }
            .foregroundStyle(UNESColor.surface)
            .padding(EdgeInsets(top: 8, leading: 14, bottom: 8, trailing: 14))
            .background(UNESColor.ink, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    // MARK: States

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("Sem horário por aqui")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text("Suas aulas aparecem assim que a primeira sincronização terminar.")
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 8) {
            Text("Não deu para carregar seu horário")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button("Tentar novamente") {
                store.send(.refreshPulled)
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(UNESColor.accent)
            .padding(.top, 8)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 300)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.26)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

#Preview {
    ScheduleView(
        store: Store(initialState: ScheduleFeature.State()) {
            ScheduleFeature()
        }
    )
}
