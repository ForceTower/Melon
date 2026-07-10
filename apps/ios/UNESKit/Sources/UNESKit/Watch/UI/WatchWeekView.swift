#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// The watch "Semana" screen: the weekly pattern day by day, Monday-first.
struct WatchWeekView: View {
    let store: StoreOf<WatchAppFeature>

    private struct Day: Identifiable {
        var id: Int { day }
        var day: Int
        var date: Date?
        var sessions: [WidgetScheduleSnapshot.Session]
    }

    var body: some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let days = days(now: now)
            List {
                ForEach(days) { day in
                    section(day, now: now)
                }
            }
        }
        .navigationTitle(Text(.watchWeekTitle))
    }

    private func days(now: Date) -> [Day] {
        let sessions = store.snapshot?.schedule.sessions ?? []
        let dates = WatchFormat.weekDates(now: now)
        let byDay = Dictionary(grouping: sessions, by: \.day)
        // Monday-first, like the Horário tab.
        return (0..<7)
            .map { offset in (offset + 1) % 7 }
            .compactMap { day in
                guard let sessions = byDay[day], !sessions.isEmpty else { return nil }
                return Day(day: day, date: dates[day], sessions: sessions.sorted { $0.startMinute < $1.startMinute })
            }
    }

    private func section(_ day: Day, now: Date) -> some View {
        let isToday = Calendar.current.component(.weekday, from: now) - 1 == day.day
        return Section {
            ForEach(day.sessions, id: \.classId) { session in
                row(session)
            }
        } header: {
            HStack(spacing: 5) {
                if isToday {
                    Circle()
                        .fill(UNESColor.coral)
                        .frame(width: 5, height: 5)
                }
                Text(day.date.map(Self.dayLabel) ?? "")
                    .font(.system(size: 12.5, weight: .bold))
                    .tracking(0.4)
                    .foregroundStyle(isToday ? UNESColor.coral : UNESColor.ink2)
                Spacer()
                Text(WatchFormat.classCount(day.sessions.count))
                    .font(.system(size: 11, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
        }
    }

    @ViewBuilder
    private func row(_ session: WidgetScheduleSnapshot.Session) -> some View {
        let row = WatchClassRow(
            time: WatchFormat.timeLabel(minutes: session.startMinute),
            title: session.title,
            subtitle: session.room.map { String.localized(.commonRoom($0)) },
            color: UNESColor.disciplineColor(session.colorIndex)
        )
        if let disciplineId = session.disciplineId {
            Button {
                store.send(.disciplineTapped(disciplineId))
            } label: {
                row
            }
        } else {
            row
        }
    }

    /// "SEG 14".
    private static func dayLabel(_ date: Date) -> String {
        "\(WatchFormat.weekdayShort(date)) \(Calendar.current.component(.day, from: date))"
    }
}

#Preview {
    NavigationStack {
        WatchWeekView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            }
        )
    }
}
#endif
