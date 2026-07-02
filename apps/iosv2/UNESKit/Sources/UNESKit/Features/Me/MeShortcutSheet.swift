import SwiftUI

/// The calendar teaser sheet behind the shortcut tile — a preview of the
/// feature the tile will eventually open.
struct MeShortcutSheet: View {
    var overview: MeOverview?
    var events: [AcademicEvent]
    var onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                calendarRows
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
            Image(systemName: MeShortcut.calendar.icon)
                .font(.system(size: 21, weight: .medium))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
                .background(MeShortcut.calendar.tone, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                .shadow(color: MeShortcut.calendar.tone.opacity(0.33), radius: 7, y: 6)

            VStack(alignment: .leading, spacing: 2) {
                Text("Calendário acadêmico")
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

    private var subtitle: String {
        let semester = overview?.semesterCode.map(DisciplinesFormat.semesterLabel)
        return [semester, "próximos eventos"].compactMap(\.self).joined(separator: " · ")
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

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        MeShortcutSheet(
            overview: .preview,
            events: .preview(),
            onClose: {}
        )
    }
}
