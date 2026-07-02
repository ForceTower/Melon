#if os(iOS)
import SwiftUI
import WidgetKit

// Lock-screen surfaces: monochrome, tinted by the system.

struct RectangularAccessoryView: View {
    var entry: NextClassEntry

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    @ViewBuilder private var content: some View {
        switch entry.status {
        case let .upcoming(occurrence):
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                        .font(.system(size: 10, weight: .semibold))
                    Text(HeroClock(for: occurrence, at: entry.date).inlineLabel)
                        .font(.system(size: 12, weight: .semibold))
                        .monospacedDigit()
                }
                .opacity(0.9)
                Text(occurrence.title)
                    .font(.system(size: 15, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .widgetAccentable()
                Text([occurrence.startTime, occurrence.room.map { "Sala \($0)" }].compactMap { $0 }.joined(separator: " · "))
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .opacity(0.8)
            }
        case let .inClass(occurrence):
            VStack(alignment: .leading, spacing: 1) {
                Text("Agora")
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .opacity(0.9)
                Text(occurrence.title)
                    .font(.system(size: 15, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .widgetAccentable()
                Text([occurrence.endTime.map { "até \($0)" }, occurrence.room].compactMap { $0 }.joined(separator: " · "))
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .opacity(0.8)
            }
        case let .dayDone(_, next):
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .bold))
                    Text("Tudo certo por hoje")
                        .font(.system(size: 12, weight: .semibold))
                }
                .opacity(0.9)
                if let next {
                    Text(next.title)
                        .font(.system(size: 15, weight: .bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .widgetAccentable()
                    Text("\(RectangularAccessoryView.untilShort(for: next, at: entry.date)) · \(next.startTime)")
                        .font(.system(size: 12, weight: .medium))
                        .monospacedDigit()
                        .opacity(0.8)
                } else {
                    Text("Sem próximas aulas")
                        .font(.system(size: 14, weight: .semibold))
                        .lineLimit(1)
                }
            }
        case .signedOut:
            VStack(alignment: .leading, spacing: 1) {
                Text("UNES")
                    .font(.system(size: 15, weight: .bold))
                    .widgetAccentable()
                Text("Toque para fazer login")
                    .font(.system(size: 12, weight: .medium))
                    .opacity(0.8)
            }
        }
    }

    /// "amanhã" / "segunda" — the compact day word for lock surfaces.
    static func untilShort(for next: ClassOccurrence, at date: Date, calendar: Calendar = .current) -> String {
        if let tomorrow = calendar.date(byAdding: .day, value: 1, to: date),
           calendar.isDate(next.start, inSameDayAs: tomorrow) {
            return "amanhã"
        }
        return HomeFormat.weekdayName(for: next.start).lowercased()
    }
}

struct CircularAccessoryView: View {
    var entry: NextClassEntry

    var body: some View {
        switch entry.status {
        case let .upcoming(occurrence):
            ZStack {
                ring(for: occurrence.start.addingTimeInterval(-3600)...occurrence.start)
                VStack(spacing: -1) {
                    Text(shortCode(occurrence.code))
                        .font(.system(size: 9, weight: .semibold))
                        .opacity(0.85)
                    Text(centerLabel(for: occurrence))
                        .font(.system(size: 16, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .widgetAccentable()
                }
                .padding(.horizontal, 8)
            }
        case let .inClass(occurrence):
            ZStack {
                ring(for: occurrence.start...occurrence.endOrEstimate, countsDown: false)
                VStack(spacing: -1) {
                    Text("fim")
                        .font(.system(size: 9, weight: .semibold))
                        .opacity(0.85)
                    Text(occurrence.endTime ?? shortCode(occurrence.code))
                        .font(.system(size: 14, weight: .bold))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                        .widgetAccentable()
                }
                .padding(.horizontal, 8)
            }
        case let .dayDone(_, next):
            VStack(spacing: -1) {
                Image(systemName: "checkmark")
                    .font(.system(size: next == nil ? 18 : 13, weight: .bold))
                    .widgetAccentable()
                if let next {
                    Text(next.startTime)
                        .font(.system(size: 11, weight: .semibold))
                        .monospacedDigit()
                        .opacity(0.85)
                }
            }
        case .signedOut:
            Image(systemName: "graduationcap.fill")
                .font(.system(size: 18, weight: .semibold))
                .widgetAccentable()
        }
    }

    /// Self-updating ring: drains over the hour before class, fills across
    /// the class itself.
    private func ring(for interval: ClosedRange<Date>, countsDown: Bool = true) -> some View {
        ProgressView(timerInterval: interval, countsDown: countsDown) {
        } currentValueLabel: {
        }
        .progressViewStyle(.circular)
    }

    /// "CALC II" → "CALC" — the ring center fits four characters well.
    private func shortCode(_ code: String) -> String {
        String(code.split(separator: " ").first.map(String.init)?.prefix(5) ?? code.prefix(5))
    }

    private func centerLabel(for occurrence: ClassOccurrence) -> String {
        switch HeroClock(for: occurrence, at: entry.date) {
        case let .minutes(big, _): big
        case .sameDay, .tomorrow, .weekday: occurrence.startTime
        }
    }
}

struct InlineAccessoryView: View {
    var entry: NextClassEntry

    var body: some View {
        switch entry.status {
        case let .upcoming(occurrence):
            let clock = HeroClock(for: occurrence, at: entry.date).inlineLabel
            ViewThatFits {
                Text([occurrence.code, occurrence.room, clock].compactMap { $0 }.joined(separator: " · "))
                Text("\(occurrence.code) · \(clock)")
                Text(clock)
            }
        case let .inClass(occurrence):
            Text(["Em aula", occurrence.endTime.map { "até \($0)" }].compactMap { $0 }.joined(separator: " · "))
        case let .dayDone(_, next):
            if let next {
                Text("Livre · \(RectangularAccessoryView.untilShort(for: next, at: entry.date)) \(next.startTime)")
            } else {
                Text("Sem próximas aulas")
            }
        case .signedOut:
            Text("UNES · faça login")
        }
    }
}

#Preview("Rectangular", as: .accessoryRectangular) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}

#Preview("Circular", as: .accessoryCircular) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}

#Preview("Inline", as: .accessoryInline) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}
#endif
