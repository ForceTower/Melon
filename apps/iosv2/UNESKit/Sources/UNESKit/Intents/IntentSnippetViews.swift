import SwiftUI

/// Static cards rendered inside the Siri / Shortcuts result sheet.
/// Purpose-built for the snippet frame — deliberately not the widget-family
/// views, which assume widget backgrounds and sizes.

/// One class: code, title, time range, room, and the posted topic.
struct IntentClassCardView: View {
    var occurrence: ClassOccurrence

    var body: some View {
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
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The day's map — every class, past ones dimmed, like the widget's
/// "Seu dia" strip.
struct IntentDayListView: View {
    var occurrences: [ClassOccurrence]
    var now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ForEach(occurrences, id: \.start) { occurrence in
                row(for: occurrence)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
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

#Preview("Class card") {
    IntentClassCardView(occurrence: .preview())
}

#Preview("Day list") {
    IntentDayListView(occurrences: ClassOccurrence.previewDay(), now: .now)
}
