import SwiftUI

/// One turma card: label, shift and seats up top, the weekly schedule and
/// meeting details, contextual banners, and the select/queue/remove footer.
struct EnrollmentSectionCard: View {
    var discipline: EnrollmentDiscipline
    var section: EnrollmentSection
    var isSelected: Bool
    var clash: EnrollmentClash?
    var useQueue: Bool
    /// Comprovante mode — the card renders without its action footer.
    var readonly = false
    var onTap: () -> Void

    /// A clash only blocks sections that aren't the current pick.
    private var isBlocked: Bool { clash != nil && !isSelected }
    private var tint: Color { discipline.tint }

    var body: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                headerRow
                EnrollmentScheduleLines(section: section)
                    .padding(.top, 12)
                meetings
                    .padding(.top, 11)

                if isBlocked, let clash {
                    EnrollmentBanner(tone: .danger, title: String.localized(.enrollmentConflictTitle)) {
                        Text(clashLine(clash))
                    }
                    .padding(.top, 12)
                }
                if section.seats.isFull, !isBlocked {
                    EnrollmentBanner(tone: .warn, title: String.localized(.enrollmentClassFullTitle)) {
                        Text(fullLine)
                    }
                    .padding(.top, 12)
                }
            }
            .padding(EdgeInsets(top: 14, leading: 15, bottom: 13, trailing: 15))

            if !readonly {
                footerButton
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(borderColor, lineWidth: isSelected ? 1.5 : 1)
        }
        .shadow(
            color: isSelected ? tint.opacity(0.16) : Color(hex: 0x141020, opacity: 0.05),
            radius: isSelected ? 13 : 9,
            y: isSelected ? 10 : 6
        )
        .animation(UNESMotion.ease(0.25), value: isSelected)
    }

    private var borderColor: Color {
        if isSelected { return tint }
        if isBlocked { return EnrollmentTone.danger.opacity(0.3) }
        return UNESColor.cardLine
    }

    // MARK: Rows

    private var headerRow: some View {
        HStack(spacing: 10) {
            Text(section.label)
                .font(.system(size: 18, weight: .bold))
                .tracking(-0.36)
                .foregroundStyle(UNESColor.ink)

            Text(EnrollmentFormat.shiftLabel(section.shift))
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.3)
                .foregroundStyle(section.hasSchedule ? tint : UNESColor.ink3)
                .padding(EdgeInsets(top: 3, leading: 8, bottom: 3, trailing: 8))
                .background(
                    section.hasSchedule ? tint.opacity(0.1) : UNESColor.surface2,
                    in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                )

            if section.coursePreferential {
                Circle()
                    .fill(EnrollmentTone.ok)
                    .frame(width: 6, height: 6)
                    .accessibilityLabel(Text(.enrollmentCoursePriority))
            }

            Spacer()

            EnrollmentSeatMeter(seats: section.seats)
        }
    }

    private var meetings: some View {
        VStack(alignment: .leading, spacing: 7) {
            ForEach(Array(section.meetings.enumerated()), id: \.offset) { _, meeting in
                HStack(spacing: 8) {
                    Text(meeting.kind)
                        .font(.system(size: 9.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 6, style: .continuous))

                    HStack(spacing: 4) {
                        Image(systemName: "house")
                            .font(.system(size: 10, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                        Text(meeting.room ?? String.localized(.enrollmentRoomTbd))
                    }

                    Text(verbatim: "·").opacity(0.35)

                    if meeting.professors.isEmpty {
                        Text(.enrollmentProfTbd)
                            .italic()
                            .foregroundStyle(UNESColor.ink4)
                    } else {
                        Text(meeting.professors.joined(separator: ", "))
                            .lineLimit(1)
                    }
                }
                .font(.system(size: 12.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            }
        }
    }

    // MARK: Footer

    private var footerButton: some View {
        Button {
            onTap()
        } label: {
            HStack(spacing: 7) {
                if let icon = footerIcon {
                    Image(systemName: icon)
                        .font(.system(size: 12.5, weight: .bold))
                }
                Text(footerLabel)
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.14)
            }
            .foregroundStyle(footerForeground)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(footerBackground)
            .overlay(alignment: .top) {
                Rectangle().fill(UNESColor.line).frame(height: 0.5)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(isBlocked || (section.seats.isFull && !useQueue && !isSelected))
    }

    private var footerLabel: String {
        if isSelected { return .localized(.enrollmentSectionCardSelectedTapToRemove) }
        if isBlocked { return .localized(.enrollmentSectionCardUnavailableConflict) }
        if section.seats.isFull { return .localized(useQueue ? .enrollmentJoinWaitlist : .enrollmentSectionCardNoSeats) }
        return .localized(.enrollmentSectionCardSelect)
    }

    private var footerIcon: String? {
        if isSelected { return "checkmark" }
        if isBlocked { return nil }
        if section.seats.isFull { return useQueue ? "person.2" : nil }
        return "plus"
    }

    private var footerForeground: Color {
        if isSelected { return .white }
        if isBlocked { return UNESColor.ink4 }
        if section.seats.isFull { return useQueue ? .white : UNESColor.ink4 }
        return UNESColor.ink
    }

    private var footerBackground: Color {
        if isSelected { return tint }
        if isBlocked { return UNESColor.surface2 }
        if section.seats.isFull, useQueue { return EnrollmentTone.warn }
        return UNESColor.surface2
    }

    // MARK: Copy

    private func clashLine(_ clash: EnrollmentClash) -> AttributedString {
        var line = AttributedString(String.localized(.enrollmentClashPrefix))
        var other = AttributedString("\(clash.discipline.code) \(clash.section.label)")
        other.font = .system(size: 12.5, weight: .bold)
        line += other
        line += AttributedString(String.localized(.enrollmentClashOn))
        var day = AttributedString(EnrollmentFormat.dayFull(clash.day))
        day.font = .system(size: 12.5, weight: .bold)
        line += day
        line += AttributedString(String.localized(.enrollmentClashSuffix))
        return line
    }

    private var fullLine: AttributedString {
        guard useQueue else { return AttributedString(String.localized(.enrollmentClassFullNoSeatsBody)) }
        var line = AttributedString(String.localized(.enrollmentWaitlistJoinInline))
        if section.waitlistCount > 0 {
            line += AttributedString(" — ")
            var ahead = AttributedString(String.localized(.enrollmentWaitlistAhead(section.waitlistCount)))
            ahead.font = .system(size: 12.5, weight: .bold)
            line += ahead
        }
        line += AttributedString(".")
        return line
    }
}

#Preview {
    let session = EnrollmentSession.preview
    let probability = session.discipline(203)!
    let concurrency = session.discipline(204)!
    ScrollView {
        VStack(spacing: 12) {
            // Selected.
            EnrollmentSectionCard(
                discipline: probability,
                section: probability.sections[1],
                isSelected: true,
                clash: nil,
                useQueue: true,
                onTap: {}
            )
            // Full → waitlist.
            EnrollmentSectionCard(
                discipline: probability,
                section: probability.sections[0],
                isSelected: false,
                clash: nil,
                useQueue: true,
                onTap: {}
            )
            // Blocked by a clash.
            EnrollmentSectionCard(
                discipline: concurrency,
                section: concurrency.sections[0],
                isSelected: false,
                clash: session.clash(with: concurrency.sections[0], excluding: concurrency.id),
                useQueue: true,
                onTap: {}
            )
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
