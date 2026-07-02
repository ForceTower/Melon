import SwiftUI

/// The teaser sheet behind a shortcut tile — a preview of the feature the
/// tile will eventually open.
struct MeShortcutSheet: View {
    var shortcut: MeShortcut
    var overview: MeOverview?
    var events: [AcademicEvent]
    var onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                content
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 24, leading: 18, bottom: 24, trailing: 18))
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .presentationCornerRadiusCompat(30)
    }

    private var header: some View {
        HStack(spacing: 12) {
            Image(systemName: shortcut.icon)
                .font(.system(size: 21, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(shortcut.tone, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .shadow(color: shortcut.tone.opacity(0.33), radius: 7, y: 6)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
                Text(subtitle)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Button(action: onClose) {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .buttonStyle(.plain)
        }
    }

    private var title: String {
        switch shortcut {
        case .calendar: "Calendário acadêmico"
        case .countdown: "Final Countdown"
        }
    }

    private var subtitle: String {
        let semester = overview?.semesterCode.map(DisciplinesFormat.semesterLabel)
        switch shortcut {
        case .calendar:
            return [semester, "próximos eventos"].compactMap(\.self).joined(separator: " · ")
        case .countdown:
            return ["fim do semestre", semester].compactMap(\.self).joined(separator: " ")
        }
    }

    @ViewBuilder
    private var content: some View {
        switch shortcut {
        case .calendar: calendarRows
        case .countdown: countdownBody
        }
    }

    // MARK: Calendar teaser

    @ViewBuilder
    private var calendarRows: some View {
        if events.isEmpty {
            Text("Sem eventos por vir no próximo mês")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 28)
        } else {
            let visible = events.prefix(5)
            VStack(spacing: 0) {
                ForEach(Array(visible.enumerated()), id: \.element.id) { position, event in
                    eventRow(event)
                        .overlay(alignment: .bottom) {
                            if position < visible.count - 1 {
                                Rectangle()
                                    .fill(UNESColor.line)
                                    .frame(height: 0.5)
                            }
                        }
                }
            }
        }
    }

    private func eventRow(_ event: AcademicEvent) -> some View {
        HStack(spacing: 14) {
            VStack(spacing: 0) {
                Text(MeFormat.eventDay(event.start))
                    .font(.system(size: 18, weight: .bold))
                    .tracking(-0.36)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                Text(MeFormat.eventMonth(event.start))
                    .textCase(.uppercase)
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(width: 44)

            VStack(alignment: .leading, spacing: 2) {
                Text(event.summary)
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.14)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)
                Text("\(MeFormat.eventWeekday(event.start)) · \(event.origin.label)")
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Circle()
                .fill(event.origin.tone)
                .frame(width: 7, height: 7)
        }
        .padding(EdgeInsets(top: 12, leading: 2, bottom: 12, trailing: 2))
    }

    // MARK: Final Countdown teaser

    @ViewBuilder
    private var countdownBody: some View {
        if let countdown = overview?.countdown {
            VStack(spacing: 10) {
                HStack(alignment: .lastTextBaseline, spacing: 10) {
                    Text("\(countdown.daysLeft)")
                        .font(.system(size: 52, weight: .bold))
                        .tracking(-2.08)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    VStack(alignment: .leading, spacing: 3) {
                        Text("dias")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(shortcut.tone)
                        Text("\(countdown.weeksLeft) semanas · \(countdown.hoursLeft)h")
                            .font(.system(size: 12, weight: .medium))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink3)
                    }
                    Spacer(minLength: 0)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(UNESColor.card)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(UNESColor.cardLine)
                }

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 2), spacing: 8) {
                    breakdownTile(label: "provas marcadas", value: countdown.scheduledExams)
                    breakdownTile(label: "aulas restantes", value: countdown.classesLeft)
                    breakdownTile(label: "fins de semana", value: countdown.weekendsLeft)
                    breakdownTile(label: "disciplinas em curso", value: countdown.disciplineCount)
                }
            }
        } else {
            Text("Sem semestre em andamento")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 28)
        }
    }

    private func breakdownTile(label: String, value: Int) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
            Text("\(value)")
                .font(.system(size: 16, weight: .bold))
                .tracking(-0.32)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

extension AcademicEvent.Origin {
    /// The dot beside a calendar teaser row.
    var tone: Color {
        switch self {
        case .evaluation, .finalExam: UNESColor.readable(0xE85D4E)
        case .secondCall: UNESColor.readable(0xE8894E)
        case .secondEpoch: UNESColor.readable(0xB23A7A)
        case .manual, .unknown: UNESColor.readable(0x2AA5B8)
        }
    }
}

extension View {
    /// `presentationCornerRadius` is iOS-only; the package also builds for macOS.
    @ViewBuilder
    func presentationCornerRadiusCompat(_ radius: CGFloat) -> some View {
        #if os(iOS)
        presentationCornerRadius(radius)
        #else
        self
        #endif
    }
}

#Preview("Calendário") {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeShortcutSheet(
            shortcut: .calendar,
            overview: .preview,
            events: .preview(),
            onClose: {}
        )
    }
}

#Preview("Final Countdown") {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeShortcutSheet(
            shortcut: .countdown,
            overview: .preview,
            events: [],
            onClose: {}
        )
    }
}
