#if os(watchOS)
import SwiftUI
import WidgetKit

// The watch complication set: the next-class hero plus the three status
// cards (Score, next exam, attendance) the Smart Stack rotates through.
// All read the structured store the phone feeds over WatchConnectivity;
// the receiver reloads every timeline when a push lands.

extension UNESWidgetKind {
    static let watchNextClass = "WatchNextClassWidget"
    static let watchCoefficient = "WatchCoefficientWidget"
    static let watchNextExam = "WatchNextExamWidget"
    static let watchAttendance = "WatchAttendanceWidget"
}

private func storedSnapshot() -> WatchSnapshot? {
    (try? WatchStore.liveValue.current()) ?? nil
}

// MARK: - Next class

/// Same timeline construction as the phone widget, fed from the watch store.
struct WatchNextClassProvider: TimelineProvider {
    func placeholder(in context: Context) -> NextClassEntry {
        .preview
    }

    func getSnapshot(in context: Context, completion: @escaping (NextClassEntry) -> Void) {
        let timeline = NextClassProvider.timeline(now: Date(), schedule: storedSnapshot()?.schedule)
        if let entry = timeline.entries.first, !context.isPreview || entry.isShowable {
            completion(entry)
        } else {
            completion(.preview)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<NextClassEntry>) -> Void) {
        completion(NextClassProvider.timeline(now: Date(), schedule: storedSnapshot()?.schedule))
    }
}

public struct WatchNextClassWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: UNESWidgetKind.watchNextClass, provider: WatchNextClassProvider()) { entry in
            WatchNextClassEntryView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName(Text(.homeHeroNextClass))
        .description(Text(.watchWidgetDescription))
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline, .accessoryCorner])
    }
}

struct WatchNextClassEntryView: View {
    var entry: NextClassEntry

    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular:
            CircularAccessoryView(entry: entry)
        case .accessoryInline:
            InlineAccessoryView(entry: entry)
        case .accessoryCorner:
            CircularAccessoryView(entry: entry)
                .widgetLabel { InlineAccessoryView(entry: entry) }
        default:
            RectangularAccessoryView(entry: entry)
        }
    }
}

// MARK: - Status widgets (Score · next exam · attendance)

struct WatchStatusEntry: TimelineEntry {
    var date: Date
    var snapshot: WatchSnapshot?
}

struct WatchStatusProvider: TimelineProvider {
    func placeholder(in context: Context) -> WatchStatusEntry {
        WatchStatusEntry(date: .now, snapshot: .preview())
    }

    func getSnapshot(in context: Context, completion: @escaping (WatchStatusEntry) -> Void) {
        let entry = WatchStatusEntry(date: Date(), snapshot: storedSnapshot())
        completion(context.isPreview && entry.snapshot == nil ? placeholder(in: context) : entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<WatchStatusEntry>) -> Void) {
        // Day-relative wording ("em 5 dias") rolls at midnight; pushes reload
        // the timeline anyway whenever the data itself changes.
        let now = Date()
        let midnight = Calendar.current.date(
            byAdding: .day,
            value: 1,
            to: Calendar.current.startOfDay(for: now)
        ) ?? now.addingTimeInterval(6 * 3600)
        completion(Timeline(
            entries: [WatchStatusEntry(date: now, snapshot: storedSnapshot())],
            policy: .after(midnight)
        ))
    }
}

public struct WatchCoefficientWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: UNESWidgetKind.watchCoefficient, provider: WatchStatusProvider()) { entry in
            WatchCoefficientView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName(Text(.widgetScore))
        .description(Text(.watchWidgetDescription))
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline])
    }
}

struct WatchCoefficientView: View {
    var entry: WatchStatusEntry

    @Environment(\.widgetFamily) private var family

    private var value: Double? { entry.snapshot?.coefficient }

    var body: some View {
        switch family {
        case .accessoryCircular:
            Gauge(value: min(max(value ?? 0, 0), 10), in: 0...10) {
                Text(.widgetScore)
            } currentValueLabel: {
                Text(formatGrade(value))
                    .font(.system(size: 16, weight: .bold))
                    .monospacedDigit()
                    .widgetAccentable()
            }
            .gaugeStyle(.accessoryCircular)
        case .accessoryInline:
            Text(verbatim: "\(String.localized(.widgetScore)) \(formatGrade(value))")
        default:
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Image(systemName: "chart.line.uptrend.xyaxis")
                        .font(.system(size: 10, weight: .semibold))
                    Text(.widgetScore)
                        .font(.system(size: 11, weight: .semibold))
                        .textCase(.uppercase)
                }
                .opacity(0.9)
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text(formatGrade(value))
                        .font(.system(size: 24, weight: .bold))
                        .monospacedDigit()
                        .widgetAccentable()
                    if let delta = entry.snapshot?.coefficientDelta, delta != 0 {
                        HStack(spacing: 1) {
                            Image(systemName: delta > 0 ? "arrow.up" : "arrow.down")
                                .font(.system(size: 10, weight: .bold))
                            Text(formatGrade(abs(delta)))
                                .font(.system(size: 13, weight: .bold))
                                .monospacedDigit()
                        }
                        .opacity(0.85)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}

public struct WatchNextExamWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: UNESWidgetKind.watchNextExam, provider: WatchStatusProvider()) { entry in
            WatchNextExamView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName(Text(.widgetNextExam))
        .description(Text(.watchWidgetDescription))
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline])
    }
}

struct WatchNextExamView: View {
    var entry: WatchStatusEntry

    @Environment(\.widgetFamily) private var family

    private var exam: WatchSnapshot.Exam? { entry.snapshot?.nextExam }

    private var daysUntil: Int? {
        exam.flatMap { WatchFormat.daysUntil(stamp: $0.date, now: entry.date) }
    }

    private var daysLabel: String? {
        daysUntil.map { days in
            switch days {
            case ..<1: String.localized(.commonToday)
            case 1: String.localized(.watchInDayOne(1))
            default: String.localized(.watchInDayOther(days))
            }
        }
    }

    var body: some View {
        switch family {
        case .accessoryCircular:
            VStack(spacing: -2) {
                if let daysUntil, daysUntil > 0 {
                    Text("\(daysUntil)")
                        .font(.system(size: 18, weight: .bold))
                        .monospacedDigit()
                        .widgetAccentable()
                    Text(verbatim: "d")
                        .font(.system(size: 10, weight: .semibold))
                        .opacity(0.85)
                } else {
                    Image(systemName: "calendar")
                        .font(.system(size: 18, weight: .semibold))
                        .widgetAccentable()
                }
            }
        case .accessoryInline:
            if let exam, let daysLabel {
                Text(verbatim: "\(exam.label) · \(exam.disciplineName) · \(daysLabel)")
            } else {
                Text(.homeExamNone)
            }
        default:
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.system(size: 10, weight: .semibold))
                    Text(.widgetNextExam)
                        .font(.system(size: 11, weight: .semibold))
                        .textCase(.uppercase)
                }
                .opacity(0.9)
                if let exam {
                    Text(verbatim: "\(exam.label) · \(exam.disciplineName)")
                        .font(.system(size: 15, weight: .bold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .widgetAccentable()
                    Text(
                        [
                            daysLabel,
                            HomeFormat.shortDate(fromDayStamp: exam.date),
                            exam.time,
                        ]
                        .compactMap(\.self)
                        .joined(separator: " · ")
                    )
                    .font(.system(size: 12, weight: .medium))
                    .monospacedDigit()
                    .opacity(0.8)
                } else {
                    Text(.homeExamNone)
                        .font(.system(size: 14, weight: .semibold))
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}

public struct WatchAttendanceWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: UNESWidgetKind.watchAttendance, provider: WatchStatusProvider()) { entry in
            WatchAttendanceView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName(Text(.widgetAttendance))
        .description(Text(.watchWidgetDescription))
        .supportedFamilies([.accessoryRectangular, .accessoryCircular, .accessoryInline])
    }
}

struct WatchAttendanceView: View {
    var entry: WatchStatusEntry

    @Environment(\.widgetFamily) private var family

    private var percent: Int? { entry.snapshot?.attendancePercent }

    var body: some View {
        switch family {
        case .accessoryCircular:
            Gauge(value: Double(percent ?? 0), in: 0...100) {
                Text(.widgetAttendance)
            } currentValueLabel: {
                Text(percent.map { "\($0)%" } ?? "—")
                    .font(.system(size: 14, weight: .bold))
                    .monospacedDigit()
                    .widgetAccentable()
            }
            .gaugeStyle(.accessoryCircular)
        case .accessoryInline:
            Text(verbatim: "\(String.localized(.widgetAttendance)) \(percent.map { "\($0)%" } ?? "—")")
        default:
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 4) {
                    Image(systemName: "flame")
                        .font(.system(size: 10, weight: .semibold))
                    Text(.widgetAttendance)
                        .font(.system(size: 11, weight: .semibold))
                        .textCase(.uppercase)
                }
                .opacity(0.9)
                Text(percent.map { "\($0)%" } ?? "—")
                    .font(.system(size: 24, weight: .bold))
                    .monospacedDigit()
                    .widgetAccentable()
                if let remaining = entry.snapshot?.remainingAbsences {
                    Text(.watchAbsencesRemaining(remaining))
                        .font(.system(size: 12, weight: .medium))
                        .monospacedDigit()
                        .opacity(0.8)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }
}
#endif
