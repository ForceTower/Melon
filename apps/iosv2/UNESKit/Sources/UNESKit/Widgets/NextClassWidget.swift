#if os(iOS)
import SwiftUI
import WidgetKit

/// "Próxima aula" — the Home v2 hero, adapted to every widget surface:
/// mesh hero before/during class, calm card once the day is done.
public struct NextClassWidget: Widget {
    public init() {}

    public var body: some WidgetConfiguration {
        StaticConfiguration(kind: UNESWidgetKind.nextClass, provider: NextClassProvider()) { entry in
            NextClassWidgetView(entry: entry)
        }
        .configurationDisplayName(.homeHeroNextClass)
        .description(.widgetDisplayDescription)
        .supportedFamilies([
            .systemSmall, .systemMedium, .systemLarge,
            .accessoryRectangular, .accessoryCircular, .accessoryInline,
        ])
    }
}

struct NextClassWidgetView: View {
    @Environment(\.widgetFamily) private var family
    var entry: NextClassEntry

    var body: some View {
        switch family {
        case .accessoryInline:
            InlineAccessoryView(entry: entry)
                .containerBackground(.clear, for: .widget)
        case .accessoryCircular:
            CircularAccessoryView(entry: entry)
                .containerBackground(for: .widget) { AccessoryWidgetBackground() }
        case .accessoryRectangular:
            RectangularAccessoryView(entry: entry)
                .containerBackground(for: .widget) { AccessoryWidgetBackground() }
        default:
            systemContent
        }
    }

    /// Home-screen families: the mesh hero stays dark in any theme (like a
    /// photo widget); the day-done card follows the system theme.
    @ViewBuilder private var systemContent: some View {
        switch entry.status {
        case let .upcoming(occurrence):
            heroContent {
                switch family {
                case .systemSmall: SmallUpcomingView(occurrence: occurrence, date: entry.date)
                case .systemLarge: LargeUpcomingView(occurrence: occurrence, entry: entry)
                default: MediumUpcomingView(occurrence: occurrence, date: entry.date)
                }
            }
        case let .inClass(occurrence):
            heroContent {
                switch family {
                case .systemSmall: SmallInClassView(occurrence: occurrence)
                case .systemLarge: LargeInClassView(occurrence: occurrence, entry: entry)
                default: MediumInClassView(occurrence: occurrence, date: entry.date)
                }
            }
        case let .dayDone(completed, next):
            DayDoneView(completed: completed, next: next, entry: entry, family: family)
                .containerBackground(UNESColor.card, for: .widget)
        case .signedOut:
            heroContent { SignedOutView(compact: family == .systemSmall) }
        }
    }

    private func heroContent(@ViewBuilder _ content: () -> some View) -> some View {
        content()
            .environment(\.colorScheme, .dark)
            .containerBackground(for: .widget) { HeroBackground() }
    }
}

// MARK: - Hero surface

/// The signature always-dark "Próxima aula" backdrop — same DNA as the Home
/// v2 hero: cool mesh over the dark base, under a diagonal scrim.
private struct HeroBackground: View {
    var body: some View {
        ZStack {
            UNESColor.darkBg
            StaticMeshView(variant: .cool)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.14), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.66), location: 1),
                ],
                angle: 155
            )
        }
    }
}

/// The cool-mesh accent used by chips and highlights over the hero.
private let heroCyan = Color(hex: 0x7FD4E0)
private let heroCyanBright = Color(hex: 0x9FE0EA)

private struct HeroEyebrow: View {
    var text: Text
    var dotColor: Color? = UNESColor.liveGreen

    var body: some View {
        HStack(spacing: 7) {
            if let dotColor {
                Circle().fill(dotColor).frame(width: 7, height: 7)
            }
            text
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.2)
                .lineLimit(1)
        }
        .foregroundStyle(.white.opacity(0.9))
    }
}

private struct CodeChip: View {
    var code: String
    var large = false

    var body: some View {
        Text(code)
            .font(.system(size: large ? 11 : 10, weight: .bold))
            .tracking(0.4)
            .foregroundStyle(heroCyan)
            .padding(.horizontal, large ? 8 : 7)
            .padding(.vertical, large ? 4 : 3)
            .background(heroCyan.opacity(0.16), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
    }
}

private struct HeroMeta: View {
    var icon: String
    var label: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .medium))
                .opacity(0.62)
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .lineLimit(1)
        }
        .foregroundStyle(.white.opacity(0.88))
    }
}

/// The room · professor line closing the medium and large heroes.
private struct HeroFooter: View {
    var occurrence: ClassOccurrence
    var teacherTitle = false

    var body: some View {
        HStack(spacing: 12) {
            if let room = occurrence.room {
                HeroMeta(icon: "mappin.and.ellipse", label: String.localized(.commonRoom(room)))
            }
            if occurrence.room != nil, occurrence.teacherName != nil {
                Rectangle().fill(.white.opacity(0.2)).frame(width: 1, height: 12)
            }
            if let teacher = occurrence.teacherName.map(HomeFormat.teacherShort) {
                HeroMeta(icon: "person", label: teacherTitle ? String.localized(.widgetProfessorPrefix(teacher)) : teacher)
            }
            Spacer(minLength: 0)
        }
        .padding(.top, 11)
        .overlay(alignment: .top) {
            Rectangle().fill(.white.opacity(0.14)).frame(height: 1)
        }
    }
}

private struct TopicLine: View {
    var topic: String
    var size: CGFloat = 13

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Image(systemName: "text.alignleft")
                .font(.system(size: size - 2, weight: .medium))
                .opacity(0.6)
            Text(topic)
                .font(.system(size: size))
                .lineLimit(1)
        }
        .foregroundStyle(.white.opacity(0.72))
    }
}

/// The right-hand time block of the medium/large heroes: big countdown with
/// a ticking "em MM:SS" line, or the absolute start farther out.
private struct HeroCountdown: View {
    var occurrence: ClassOccurrence
    var date: Date
    var bigSize: CGFloat = 34
    var unitSize: CGFloat = 15

    var body: some View {
        VStack(alignment: .trailing, spacing: 2) {
            switch HeroClock(for: occurrence, at: date) {
            case let .minutes(big, unit):
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    bigText(big)
                    if let unit {
                        Text(unit)
                            .font(.system(size: unitSize, weight: .semibold))
                            .opacity(0.7)
                    }
                }
                if date < occurrence.start {
                    // Timer-interval text greedily claims the whole row —
                    // bound it so the line hugs the number's right edge and
                    // stops squeezing the title column.
                    (Text(.widgetInPrefix) + Text(timerInterval: date...occurrence.start, countsDown: true))
                        .font(.system(size: 11, weight: .semibold))
                        .monospacedDigit()
                        .opacity(0.6)
                        .lineLimit(1)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 76, alignment: .trailing)
                }
            case let .sameDay(time):
                bigText(time)
                subText(String.localized(.widgetToday))
            case let .tomorrow(time):
                bigText(time)
                subText(String.localized(.widgetTomorrow))
            case let .weekday(name, time):
                bigText(name)
                subText(time)
            }
        }
        .foregroundStyle(.white)
    }

    private func bigText(_ value: String) -> some View {
        Text(value)
            .font(.system(size: bigSize, weight: .bold))
            .tracking(-0.04 * bigSize)
            .monospacedDigit()
            .lineLimit(1)
    }

    private func subText(_ value: String) -> some View {
        Text(value)
            .font(.system(size: 11, weight: .semibold))
            .tracking(0.4)
            .monospacedDigit()
            .opacity(0.6)
    }
}

// MARK: - Upcoming (the mesh hero)

private struct SmallUpcomingView: View {
    var occurrence: ClassOccurrence
    var date: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HeroEyebrow(text: Text(.widgetNext))
            Spacer(minLength: 8)
            CodeChip(code: occurrence.code)
            Text(occurrence.title)
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.72)
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
                .padding(.top, 7)
            HStack {
                Text(HeroClock(for: occurrence, at: date).inlineLabel)
                    .foregroundStyle(.white.opacity(0.82))
                Spacer(minLength: 8)
                if let room = occurrence.room {
                    Text(room)
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
            }
            .font(.system(size: 13, weight: .semibold))
            .monospacedDigit()
            .padding(.top, 9)
            .overlay(alignment: .top) {
                Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
            }
            .padding(.top, 10)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

private struct MediumUpcomingView: View {
    var occurrence: ClassOccurrence
    var date: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HeroEyebrow(text: Text(.homeHeroNextClass))
                Spacer()
                Text(occurrence.timeRange)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.white.opacity(0.6))
            }
            Spacer(minLength: 8)
            HStack(alignment: .bottom, spacing: 14) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(occurrence.title)
                        .font(.system(size: 27, weight: .bold))
                        .tracking(-0.81)
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    if let topic = occurrence.topic {
                        TopicLine(topic: topic)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                HeroCountdown(occurrence: occurrence, date: date)
            }
            HeroFooter(occurrence: occurrence)
                .padding(.top, 12)
        }
    }
}

private struct LargeUpcomingView: View {
    var occurrence: ClassOccurrence
    var entry: NextClassEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HeroEyebrow(text: Text(.homeHeroNextClass))
                Spacer()
                Text(occurrence.timeRange)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.white.opacity(0.6))
            }
            HStack(alignment: .bottom, spacing: 14) {
                VStack(alignment: .leading, spacing: 9) {
                    CodeChip(code: occurrence.code, large: true)
                    Text(occurrence.title)
                        .font(.system(size: 32, weight: .bold))
                        .tracking(-1.12)
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                HeroCountdown(occurrence: occurrence, date: entry.date, bigSize: 40, unitSize: 17)
            }
            .padding(.top, 14)
            if let topic = occurrence.topic {
                TopicLine(topic: topic, size: 13.5)
                    .padding(.top, 12)
            }
            Spacer(minLength: 12)
            DayStrip(today: entry.today, date: entry.date, highlighted: occurrence)
            Spacer(minLength: 12)
            HeroFooter(occurrence: occurrence, teacherTitle: true)
        }
    }
}

/// The translucent "Seu dia" timeline on the large hero.
private struct DayStrip: View {
    var today: [ClassOccurrence]
    var date: Date
    var highlighted: ClassOccurrence?

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(.homeSectionYourDay)
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .semibold))
                .tracking(0.55)
                .foregroundStyle(.white.opacity(0.5))
            HStack(spacing: 6) {
                ForEach(today.prefix(5), id: \.start) { occurrence in
                    chip(for: occurrence)
                }
            }
        }
    }

    private func chip(for occurrence: ClassOccurrence) -> some View {
        let done = occurrence.endOrEstimate <= date
        let next = occurrence.start == highlighted?.start
        return VStack(alignment: .leading, spacing: 3) {
            Text(occurrence.code)
                .font(.system(size: 10, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(next ? heroCyanBright : .white.opacity(0.7))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(occurrence.startTime)
                .font(.system(size: 11, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(.white.opacity(0.6))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 9)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            next ? heroCyan.opacity(0.22) : .white.opacity(0.08),
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(next ? heroCyan.opacity(0.5) : .white.opacity(0.08), lineWidth: 1)
        }
        .opacity(done ? 0.45 : 1)
    }
}

// MARK: - In class (live progress)

private struct SmallInClassView: View {
    var occurrence: ClassOccurrence

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HeroEyebrow(text: Text(.commonNow))
            Spacer(minLength: 8)
            CodeChip(code: occurrence.code)
            Text(occurrence.title)
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.72)
                .foregroundStyle(.white)
                .lineLimit(2)
                .minimumScaleFactor(0.7)
                .padding(.top, 7)
            HStack {
                if let end = occurrence.endTime {
                    Text(.widgetUntilTime(end))
                        .foregroundStyle(.white.opacity(0.82))
                }
                Spacer(minLength: 8)
                if let room = occurrence.room {
                    Text(room)
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
            }
            .font(.system(size: 13, weight: .semibold))
            .monospacedDigit()
            .padding(.top, 9)
            .overlay(alignment: .top) {
                Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
            }
            .padding(.top, 10)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

/// "10:20 · iniciada" / "12:00 · fim" around a self-updating progress bar.
private struct ClassProgress: View {
    var occurrence: ClassOccurrence

    var body: some View {
        VStack(spacing: 7) {
            ProgressView(timerInterval: occurrence.start...occurrence.endOrEstimate, countsDown: false) {
            } currentValueLabel: {
            }
            .tint(UNESColor.liveGreen)
            HStack {
                Text(.homeHeroStarted(occurrence.startTime))
                Spacer()
                if let end = occurrence.endTime {
                    Text(.homeHeroEnded(end))
                }
            }
            .font(.system(size: 12, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(.white.opacity(0.6))
        }
    }
}

private struct MediumInClassView: View {
    var occurrence: ClassOccurrence
    var date: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HeroEyebrow(text: eyebrowText)
                Spacer()
                Text(occurrence.timeRange)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.white.opacity(0.6))
            }
            Spacer(minLength: 8)
            CodeChip(code: occurrence.code)
            Text(occurrence.title)
                .font(.system(size: 27, weight: .bold))
                .tracking(-0.81)
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.top, 7)
            ClassProgress(occurrence: occurrence)
                .padding(.top, 12)
        }
    }

    private var eyebrowText: Text {
        if date < occurrence.endOrEstimate {
            Text(.widgetNowEndsInPrefix)
                + Text(timerInterval: date...occurrence.endOrEstimate, countsDown: true)
        } else {
            Text(.commonNow)
        }
    }
}

private struct LargeInClassView: View {
    var occurrence: ClassOccurrence
    var entry: NextClassEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                HeroEyebrow(text: Text(.commonNow))
                Spacer()
                Text(occurrence.timeRange)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.white.opacity(0.6))
            }
            VStack(alignment: .leading, spacing: 9) {
                CodeChip(code: occurrence.code, large: true)
                Text(occurrence.title)
                    .font(.system(size: 30, weight: .bold))
                    .tracking(-1.05)
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.7)
            }
            .padding(.top, 14)
            if let topic = occurrence.topic {
                TopicLine(topic: topic, size: 13.5)
                    .padding(.top, 12)
            }
            ClassProgress(occurrence: occurrence)
                .padding(.top, 14)
            Spacer(minLength: 12)
            DayStrip(today: entry.today, date: entry.date, highlighted: occurrence)
            Spacer(minLength: 12)
            HeroFooter(occurrence: occurrence, teacherTitle: true)
        }
    }
}

// MARK: - Day done (theme-aware card)

private struct DayDoneView: View {
    var completed: Int
    var next: ClassOccurrence?
    var entry: NextClassEntry
    var family: WidgetFamily

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            eyebrow
            Spacer(minLength: 8)
            title
                .lineLimit(family == .systemSmall ? 3 : 2)
                .minimumScaleFactor(0.7)
            if family != .systemSmall, let next {
                Text([next.title, next.room].compactMap { $0 }.joined(separator: " · "))
                    .font(.system(size: 13.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
                    .padding(.top, 5)
            }
            if family == .systemLarge {
                Spacer(minLength: 12)
                CardDayStrip(today: entry.today)
            }
            if completed > 0 {
                footer
                    .padding(.top, 11)
                    .overlay(alignment: .top) {
                        Rectangle().fill(UNESColor.line).frame(height: 1)
                    }
                    .padding(.top, family == .systemSmall ? 10 : 12)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    private var eyebrow: some View {
        HStack(spacing: 7) {
            Image(systemName: "checkmark")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(UNESColor.successGreen)
            (completed > 0 ? Text(.widgetAllDoneToday) : Text(.homeDayEmpty))
                .textCase(.uppercase)
                .font(.system(size: family == .systemSmall ? 11 : 12, weight: .semibold))
                .tracking(0.2)
                .foregroundStyle(UNESColor.ink3)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
    }

    private var title: some View {
        let size: CGFloat = family == .systemSmall ? 18 : 22
        let text: Text
        if let next {
            text = Text(.widgetNoClassesUntilPrefix)
                + Text(untilLabel(for: next)).foregroundStyle(UNESColor.accent)
        } else {
            text = Text(.widgetNoUpcomingClasses)
        }
        return text
            .font(.system(size: size, weight: .bold))
            .tracking(-0.03 * size)
            .foregroundStyle(UNESColor.ink)
    }

    private var footer: some View {
        HStack {
            Text(.widgetClassesCompleted(completed))
                .font(.system(size: family == .systemSmall ? 12 : 13, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer(minLength: 6)
            HStack(spacing: 5) {
                ForEach(0..<min(completed, 6), id: \.self) { _ in
                    Circle().fill(UNESColor.successGreen).frame(width: 7, height: 7)
                }
            }
        }
    }

    /// "amanhã, 08:00" / "segunda, 08:00" — matching the design's accent span.
    private func untilLabel(for next: ClassOccurrence, calendar: Calendar = .current) -> String {
        if let tomorrow = calendar.date(byAdding: .day, value: 1, to: entry.date),
           calendar.isDate(next.start, inSameDayAs: tomorrow) {
            return "\(String.localized(.widgetTomorrow)), \(next.startTime)"
        }
        return "\(HomeFormat.weekdayName(for: next.start).lowercased()), \(next.startTime)"
    }
}

/// The "Seu dia" strip restyled for the light day-done card.
private struct CardDayStrip: View {
    var today: [ClassOccurrence]

    var body: some View {
        if today.isEmpty {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: 9) {
                Text(.homeSectionYourDay)
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.55)
                    .foregroundStyle(UNESColor.ink4)
                HStack(spacing: 6) {
                    ForEach(today.prefix(5), id: \.start) { occurrence in
                        VStack(alignment: .leading, spacing: 3) {
                            Text(occurrence.code)
                                .font(.system(size: 10, weight: .bold))
                                .tracking(0.3)
                                .foregroundStyle(UNESColor.ink3)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                            Text(occurrence.startTime)
                                .font(.system(size: 11, weight: .medium))
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink4)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 9)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
            }
        }
    }
}

// MARK: - Signed out

private struct SignedOutView: View {
    var compact: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HeroEyebrow(text: Text(verbatim: "UNES"), dotColor: nil)
            Spacer(minLength: 8)
            (compact ? Text(.widgetSignInCompact) : Text(.widgetSignInToSeeNextClass))
                .font(.system(size: compact ? 20 : 22, weight: .bold))
                .tracking(-0.6)
                .foregroundStyle(.white)
                .lineLimit(3)
                .minimumScaleFactor(0.8)
            (compact ? Text(.widgetTapToOpen) : Text(.widgetTapToOpenAndSignIn))
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white.opacity(0.7))
                .lineLimit(2)
                .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

// MARK: - Previews

#Preview("Small", as: .systemSmall) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}

#Preview("Medium", as: .systemMedium) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}

#Preview("Large", as: .systemLarge) {
    NextClassWidget()
} timeline: {
    NextClassEntry.preview
    NextClassEntry.previewInClass
    NextClassEntry.previewDayDone
}
#endif
