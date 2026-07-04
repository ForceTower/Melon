#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// The watch "Hoje" screen: the next-class mesh hero and the day's schedule.
struct WatchHomeView: View {
    let store: StoreOf<WatchAppFeature>

    var body: some View {
        Group {
            if let snapshot = store.snapshot {
                loaded(snapshot)
            } else if store.hasLoaded {
                WatchSignedOutView()
            } else {
                ProgressView()
            }
        }
        .navigationTitle(Text(.commonToday))
    }

    private func loaded(_ snapshot: WatchSnapshot) -> some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let read = NextClassStatus.compute(
                at: now,
                occurrences: snapshot.schedule.occurrences(from: now, days: 8),
                calendar: .current
            )
            List {
                Section {
                    WatchHeroCard(status: read.status, now: now) { id in
                        store.send(.disciplineTapped(id))
                    }
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
                }
                daySection(read.today, now: now)
                Section {
                    Text(HomeFormat.updatedLabel(lastRefreshed: snapshot.syncedAt, now: now))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .frame(maxWidth: .infinity)
                        .listRowBackground(Color.clear)
                }
            }
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    store.send(.weekTapped)
                } label: {
                    Image(systemName: "calendar")
                        .foregroundStyle(.white)
                }
                .accessibilityLabel(Text(.watchWeekTitle))
            }
        }
    }

    @ViewBuilder
    private func daySection(_ today: [ClassOccurrence], now: Date) -> some View {
        Section {
            if today.isEmpty {
                Text(.homeDayEmpty)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            } else {
                let nowIndex = today.firstIndex { $0.start > now } ?? today.count
                ForEach(Array(today.enumerated()), id: \.element.classId) { index, occurrence in
                    if index == nowIndex {
                        nowLine(now)
                    }
                    row(occurrence, now: now)
                }
                if nowIndex == today.count {
                    nowLine(now)
                }
            }
        } header: {
            Text(.watchYourDay)
                .font(.system(size: 12, weight: .bold))
                .tracking(0.4)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private func nowLine(_ now: Date) -> some View {
        WatchNowLine(now: now)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4))
    }

    @ViewBuilder
    private func row(_ occurrence: ClassOccurrence, now: Date) -> some View {
        let isDone = now >= occurrence.endOrEstimate
        let isNow = occurrence.start <= now && now < occurrence.endOrEstimate
        let row = WatchClassRow(
            time: occurrence.startTime,
            title: occurrence.title,
            subtitle: occurrence.topic
                ?? occurrence.room.map { String.localized(.commonRoom($0)) },
            color: UNESColor.disciplineColor(occurrence.colorIndex),
            isDone: isDone,
            isNow: isNow
        )
        if let disciplineId = occurrence.disciplineId {
            Button {
                store.send(.disciplineTapped(disciplineId))
            } label: {
                row
            }
        } else {
            row
        }
    }
}

/// The next-class hero: live countdown, running progress, or the day-done
/// wind-down, on the schedule's cool mesh.
struct WatchHeroCard: View {
    var status: NextClassStatus
    var now: Date
    var onOpen: (String) -> Void

    var body: some View {
        switch status {
        case let .upcoming(occurrence):
            open(occurrence) {
                WatchMeshCard(variant: .cool) {
                    VStack(alignment: .leading, spacing: 7) {
                        eyebrow("\(String.localized(.homeHeroNextClass)) · \(occurrence.startTime)")
                        HStack(alignment: .bottom, spacing: 8) {
                            VStack(alignment: .leading, spacing: 2) {
                                title(occurrence.title)
                                if let topic = occurrence.topic {
                                    Text(topic)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundStyle(.white.opacity(0.8))
                                        .lineLimit(2)
                                }
                            }
                            Spacer(minLength: 6)
                            VStack(alignment: .trailing, spacing: 1) {
                                Text(.watchHeroStartsIn)
                                    .font(.system(size: 9.5, weight: .semibold))
                                    .tracking(0.3)
                                    .textCase(.uppercase)
                                    .foregroundStyle(.white.opacity(0.6))
                                countdown(to: occurrence.start)
                            }
                        }
                        footer(occurrence)
                    }
                }
            }

        case let .inClass(occurrence):
            open(occurrence) {
                WatchMeshCard(variant: .cool) {
                    VStack(alignment: .leading, spacing: 7) {
                        eyebrow(String.localized(.commonNow))
                        title(occurrence.title)
                        ProgressView(timerInterval: occurrence.start...occurrence.endOrEstimate) {
                        } currentValueLabel: {
                        }
                        .tint(.white)
                        footer(occurrence)
                    }
                }
            }

        case let .dayDone(_, next):
            WatchMeshCard(variant: .fresh) {
                VStack(alignment: .leading, spacing: 5) {
                    title(String.localized(.widgetAllDoneToday))
                    if let next {
                        Text(.watchHeroNext("\(next.title) · \(WatchFormat.weekdayShort(next.start)) \(next.startTime)"))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.8))
                            .lineLimit(2)
                    }
                }
            }

        case .signedOut:
            EmptyView()
        }
    }

    @ViewBuilder
    private func open(_ occurrence: ClassOccurrence, @ViewBuilder card: () -> some View) -> some View {
        if let disciplineId = occurrence.disciplineId {
            Button {
                onOpen(disciplineId)
            } label: {
                card()
            }
            .buttonStyle(.plain)
        } else {
            card()
        }
    }

    private func eyebrow(_ text: String) -> some View {
        HStack(spacing: 5) {
            Circle()
                .fill(UNESColor.liveGreen)
                .frame(width: 5, height: 5)
            Text(text)
                .font(.system(size: 10.5, weight: .bold))
                .tracking(0.5)
                .textCase(.uppercase)
                .foregroundStyle(.white.opacity(0.9))
        }
    }

    private func title(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 18, weight: .bold))
            .tracking(-0.4)
            .foregroundStyle(.white)
            .lineLimit(2)
    }

    @ViewBuilder
    private func countdown(to start: Date) -> some View {
        if start.timeIntervalSince(now) < 70 * 60 {
            Text(timerInterval: now...start, countsDown: true, showsHours: false)
                .font(.system(size: 20, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(.white)
        } else {
            Text(start, style: .time)
                .font(.system(size: 20, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(.white)
        }
    }

    @ViewBuilder
    private func footer(_ occurrence: ClassOccurrence) -> some View {
        let pieces = [
            occurrence.room.map { String.localized(.commonRoom($0)) },
            occurrence.teacherName.map(ScheduleFormat.shortTeacherName),
        ].compactMap(\.self)
        if !pieces.isEmpty {
            VStack(alignment: .leading, spacing: 6) {
                Rectangle()
                    .fill(.white.opacity(0.14))
                    .frame(height: 0.5)
                HStack(spacing: 4) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(.white.opacity(0.66))
                    Text(pieces.joined(separator: " · "))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.85))
                        .lineLimit(1)
                }
            }
        }
    }
}

/// Shown until the phone pushes a first payload.
struct WatchSignedOutView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Image(systemName: "iphone.gen3.radiowaves.left.and.right")
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(UNESColor.coral)
                Text(.watchSignedOutTitle)
                    .font(.system(size: 15, weight: .semibold))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(UNESColor.ink)
                Text(.watchSignedOutBody)
                    .font(.system(size: 12.5, weight: .medium))
                    .multilineTextAlignment(.center)
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 8)
        }
    }
}

#Preview {
    NavigationStack {
        WatchHomeView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            }
        )
    }
}

#Preview("Signed out") {
    NavigationStack {
        WatchHomeView(
            store: Store(initialState: WatchAppFeature.State(hasLoaded: true)) {
                WatchAppFeature()
            }
        )
    }
}
#endif
