import SwiftUI

/// Static cards rendered inside the Siri / Shortcuts result sheet.
/// Purpose-built for the snippet frame — deliberately not the widget-family
/// views, which assume widget backgrounds and sizes.

/// Shared card chrome. Snippets composite onto a clear background over the
/// sheet's blur, so without an opaque surface the content is illegible —
/// same treatment as the widget's day-done card.
private struct SnippetCard<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine, lineWidth: 1)
            }
    }
}

/// One class: code, title, time range, room, and the posted topic.
public struct IntentClassCardView: View {
    var occurrence: ClassOccurrence

    public var body: some View {
        SnippetCard {
            classContent
        }
    }

    private var classContent: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(occurrence.code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(UNESColor.accent.opacity(0.14), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                Spacer(minLength: 8)
                Text(occurrence.timeRange)
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink3)
            }
            Text(occurrence.title)
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(2)
                .minimumScaleFactor(0.8)
            if let room = occurrence.room {
                HStack(spacing: 6) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                    Text(String.localized(.commonRoom(room)))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
            }
            if let topic = occurrence.topic {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Image(systemName: "text.alignleft")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                    Text(topic)
                        .font(.system(size: 13))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(2)
                }
            }
        }
    }
}

/// The day's map — every class, past ones dimmed, like the widget's
/// "Seu dia" strip.
public struct IntentDayListView: View {
    var occurrences: [ClassOccurrence]
    var now: Date

    public var body: some View {
        SnippetCard {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(occurrences, id: \.start) { occurrence in
                    row(for: occurrence)
                }
            }
        }
    }

    private func row(for occurrence: ClassOccurrence) -> some View {
        let done = occurrence.endOrEstimate <= now
        return HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text(occurrence.startTime)
                .font(.system(size: 13, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
            VStack(alignment: .leading, spacing: 1) {
                Text(occurrence.title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                if let room = occurrence.room {
                    Text(room)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .opacity(done ? 0.45 : 1)
    }
}

/// The score: big truncated value, trend sparkline, delta chip — the Me
/// hero's stat, reshaped for the snippet frame.
public struct IntentScoreCardView: View {
    var coefficient: CoefficientSummary

    public var body: some View {
        SnippetCard {
            HStack(alignment: .center, spacing: 16) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(.meStatScore)
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(0.5)
                        .foregroundStyle(UNESColor.ink4)
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        Text(formatGrade(coefficient.value))
                            .font(.system(size: 40, weight: .bold))
                            .tracking(-1.2)
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink)
                        if let delta {
                            HStack(spacing: 3) {
                                Image(systemName: delta.rising ? "arrow.up" : "arrow.down")
                                    .font(.system(size: 11, weight: .bold))
                                Text(delta.text)
                                    .font(.system(size: 13, weight: .semibold))
                                    .monospacedDigit()
                            }
                            .foregroundStyle(delta.rising ? UNESColor.successGreen : UNESColor.coral)
                        }
                    }
                }
                Spacer(minLength: 0)
                if coefficient.spark.count > 1 {
                    Sparkline(values: coefficient.spark, color: UNESColor.accent)
                }
            }
        }
    }

    /// The Me hero's delta rule: hidden while the movement rounds to zero.
    private var delta: (text: String, rising: Bool)? {
        guard let delta = coefficient.delta, abs(delta) >= 0.1 else { return nil }
        let rising = delta >= 0
        return ("\(rising ? "+" : "−")\(formatGrade(abs(delta)))", rising)
    }
}

/// The newest unread messages: sender, subject (or body preview), and a
/// relative date per row.
public struct IntentUnreadListView: View {
    var items: [MessageItem]

    public var body: some View {
        SnippetCard {
            VStack(alignment: .leading, spacing: 12) {
                ForEach(items) { message in
                    row(for: message)
                }
            }
        }
    }

    private func row(for message: MessageItem) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(message.senderName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text(message.receivedAt.formatted(.relative(presentation: .named)))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            Text(message.subject ?? message.body)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .lineLimit(2)
        }
    }
}

/// The Prova Final verdict: the actionable number big, its meaning as the
/// caption, and the supporting average / next-evaluation line underneath.
public struct IntentVerdictCardView: View {
    var code: String
    var name: String
    var verdict: FinalExamVerdict

    public var body: some View {
        SnippetCard {
            VStack(alignment: .leading, spacing: 7) {
                Text(code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.accent)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(UNESColor.accent.opacity(0.14), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                Text(name)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(bigValue)
                        .font(.system(size: 34, weight: .bold))
                        .tracking(-1)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    Text(caption)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                if let support {
                    Text(support)
                        .font(.system(size: 13, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
    }

    private var bigValue: String {
        switch verdict {
        case .stale, .noGrades: "—"
        case let .approved(average), let .lost(average): formatGrade(average)
        case let .finals(_, needed), let .directClose(_, needed, _): DisciplinesFormat.neededGrade(needed)
        case let .finalsPath(average), let .partial(average): formatGrade(average)
        }
    }

    private var caption: LocalizedStringResource {
        switch verdict {
        case .finals: .intentFinalExamCardNeeded
        case .directClose: .intentFinalExamCardNeededDirect
        default: .intentFinalExamCardAverage
        }
    }

    private var support: String? {
        switch verdict {
        case let .finals(average, _):
            return String.localized(.intentFinalExamCardAverageLine(formatGrade(average)))
        case let .directClose(average, _, next):
            var parts = [String.localized(.intentFinalExamCardAverageLine(formatGrade(average)))]
            if let next {
                parts.append("\(next.title) · \(DisciplinesFormat.shortDate(next.dateStamp))")
            }
            return parts.joined(separator: "  ·  ")
        default:
            return nil
        }
    }
}

#Preview("Class card") {
    IntentClassCardView(occurrence: .preview())
}

#Preview("Day list") {
    IntentDayListView(occurrences: ClassOccurrence.previewDay(), now: .now)
}

#Preview("Score card") {
    IntentScoreCardView(
        coefficient: CoefficientSummary(value: 8.5, spark: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5], delta: 0.3)
    )
}

#Preview("Unread list") {
    IntentUnreadListView(items: Array(MessagesOverview.preview().messages.filter(\.unread).prefix(3)))
}

#Preview("Verdict card") {
    IntentVerdictCardView(code: "MAT202", name: "Cálculo II", verdict: .finals(average: 5.8, needed: 4.3))
}
