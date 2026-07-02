import SwiftUI

// Shared primitives of the matrícula screens: the card chrome, code chips,
// pill badges, banners, seat meters and schedule lines every step reuses.

/// Status tones of the flow — dark-readable like the discipline palette.
enum EnrollmentTone {
    static let danger = UNESColor.readable(0xE85D4E)
    static let warn = UNESColor.readable(0xD9852E)
    static let ok = UNESColor.readable(0x2F9E5E)
}

extension EnrollmentDiscipline {
    var tint: Color { UNESColor.disciplineReadableColor(colorIndex) }
}

// MARK: - Card chrome

private struct EnrollmentCardModifier: ViewModifier {
    var radius: CGFloat

    func body(content: Content) -> some View {
        content
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

extension View {
    /// The grouped-card treatment shared by every matrícula surface.
    func enrollmentCard(radius: CGFloat = 20) -> some View {
        modifier(EnrollmentCardModifier(radius: radius))
    }
}

// MARK: - Ambient wash

/// Faint mesh washing down from behind the large title — the shared backdrop
/// of every matrícula screen.
struct EnrollmentAmbientWash: View {
    var variant: MeshView.Variant
    var opacity: Double

    var body: some View {
        MeshView(variant: variant, intensity: 0.5)
            .frame(height: 320)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(opacity)
            .offset(y: -80)
            .ignoresSafeArea()
            .allowsHitTesting(false)
    }
}

// MARK: - Section header

struct EnrollmentSectionHeader: View {
    var title: LocalizedStringResource
    var action: LocalizedStringResource?
    var onAction: (() -> Void)?

    var body: some View {
        HStack(alignment: .lastTextBaseline) {
            Text(title)
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.6)
                .foregroundStyle(UNESColor.ink)
            Spacer()
            if let action {
                Button { onAction?() } label: { Text(action) }
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(UNESColor.accent)
                    .buttonStyle(.plain)
            }
        }
        .padding(EdgeInsets(top: 0, leading: 2, bottom: 12, trailing: 2))
    }
}

// MARK: - Code chip

struct EnrollmentCodeChip: View {
    var code: String
    var color: Color
    var small = false

    var body: some View {
        Text(code)
            .font(.system(size: small ? 9.5 : 10.5, weight: .bold))
            .tracking(0.4)
            .monospacedDigit()
            .foregroundStyle(color)
            .padding(EdgeInsets(top: small ? 2 : 3, leading: small ? 6 : 7, bottom: small ? 2 : 3, trailing: small ? 6 : 7))
            .background(color.opacity(0.13), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
    }
}

// MARK: - Pill badge

struct EnrollmentBadge: View {
    enum Kind {
        case mandatory, optional, suggested, prereq, waitlist, selected
    }

    var kind: Kind
    var text: String

    var body: some View {
        HStack(spacing: 5) {
            switch kind {
            case .suggested, .waitlist:
                Circle().fill(foreground).frame(width: 5, height: 5)
            case .prereq:
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 8, weight: .bold))
            case .selected:
                Image(systemName: "checkmark")
                    .font(.system(size: 9, weight: .bold))
            case .mandatory, .optional:
                EmptyView()
            }
            Text(text)
        }
        .font(.system(size: 10.5, weight: .semibold))
        .foregroundStyle(foreground)
        .padding(EdgeInsets(top: 3, leading: 8, bottom: 3, trailing: 8))
        .background(background, in: Capsule())
        .overlay {
            if kind == .optional {
                Capsule().strokeBorder(UNESColor.line)
            }
        }
        .lineLimit(1)
        .fixedSize()
    }

    private var foreground: Color {
        switch kind {
        case .mandatory: UNESColor.ink2
        case .optional: UNESColor.ink3
        case .suggested: EnrollmentTone.ok
        case .prereq: EnrollmentTone.danger
        case .waitlist: EnrollmentTone.warn
        case .selected: .white
        }
    }

    private var background: Color {
        switch kind {
        case .mandatory: UNESColor.surface2
        case .optional: .clear
        case .suggested: EnrollmentTone.ok.opacity(0.15)
        case .prereq: EnrollmentTone.danger.opacity(0.15)
        case .waitlist: EnrollmentTone.warn.opacity(0.16)
        case .selected: UNESColor.accent
        }
    }
}

// MARK: - Banner

struct EnrollmentBanner<Content: View>: View {
    enum Tone {
        case danger, warn, info, neutral
    }

    var tone: Tone
    var title: String?
    var action: String?
    var onAction: (() -> Void)?
    @ViewBuilder var content: Content

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            Image(systemName: icon)
                .font(.system(size: 10.5, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 22, height: 22)
                .background(color, in: Circle())
                .padding(.top, 1)

            VStack(alignment: .leading, spacing: 2) {
                if let title {
                    Text(title)
                        .font(.system(size: 13.5, weight: .semibold))
                        .tracking(-0.13)
                        .foregroundStyle(UNESColor.ink)
                }
                content
                    .font(.system(size: 12.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(UNESColor.ink2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let action {
                    Button {
                        onAction?()
                    } label: {
                        Text("\(action) →")
                            .font(.system(size: 12.5, weight: .semibold))
                            .foregroundStyle(color)
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 5)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
        .background(fill, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(border)
        }
    }

    private var color: Color {
        switch tone {
        case .danger: EnrollmentTone.danger
        case .warn: EnrollmentTone.warn
        case .info: EnrollmentTone.ok
        case .neutral: UNESColor.ink3
        }
    }

    private var icon: String {
        switch tone {
        case .danger, .warn: "exclamationmark.triangle.fill"
        case .info: "checkmark"
        case .neutral: "info"
        }
    }

    private var fill: Color {
        tone == .neutral ? UNESColor.surface2 : color.opacity(0.12)
    }

    private var border: Color {
        tone == .neutral ? UNESColor.line : color.opacity(0.26)
    }
}

// MARK: - Seat meter

struct EnrollmentSeatMeter: View {
    var seats: EnrollmentSeats

    var body: some View {
        VStack(alignment: .trailing, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text("\(seats.filled)")
                    .font(.system(size: 15, weight: .bold))
                    .tracking(-0.3)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                Text("/ \(seats.total)")
                    .font(.system(size: 11, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }

            GeometryReader { geometry in
                Capsule()
                    .fill(color)
                    .frame(width: min(1, seats.fraction) * geometry.size.width)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(height: 4)
            .background(UNESColor.surface3, in: Capsule())

            Text(caption)
                .textCase(.uppercase)
                .font(.system(size: 9.5, weight: .bold))
                .tracking(0.3)
                .foregroundStyle(color)
        }
        .frame(width: 66)
    }

    private var color: Color {
        if seats.isFull { return EnrollmentTone.danger }
        if seats.isTight { return EnrollmentTone.warn }
        return EnrollmentTone.ok
    }

    private var caption: String {
        if seats.isFull { return .localized(.enrollmentSeatsFull) }
        if seats.isTight { return .localized(.enrollmentSeatsTight) }
        return .localized(.enrollmentSeatsOpen)
    }
}

// MARK: - Schedule lines

/// "Seg, Qua  13:30–15:30" rows, or the "a definir" placeholder.
struct EnrollmentScheduleLines: View {
    var section: EnrollmentSection

    var body: some View {
        if section.hasSchedule {
            VStack(alignment: .leading, spacing: 3) {
                ForEach(EnrollmentFormat.scheduleLines(for: section), id: \.time) { line in
                    HStack(spacing: 7) {
                        Text(line.days)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(UNESColor.ink2)
                        Text(line.time)
                            .font(.system(size: 13, weight: .medium))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
            }
        } else {
            HStack(spacing: 6) {
                Image(systemName: "clock")
                    .font(.system(size: 11, weight: .medium))
                Text(.enrollmentScheduleTbd)
                    .font(.system(size: 12.5, weight: .medium))
            }
            .foregroundStyle(UNESColor.ink4)
        }
    }
}

#Preview {
    ScrollView {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                EnrollmentCodeChip(code: "EXA427", color: UNESColor.disciplineReadableColor(1))
                EnrollmentBadge(kind: .suggested, text: .localized(.enrollmentBadgeSuggested))
                EnrollmentBadge(kind: .mandatory, text: .localized(.enrollmentBadgeMandatory))
                EnrollmentBadge(kind: .optional, text: .localized(.enrollmentBadgeOptional))
            }
            HStack(spacing: 8) {
                EnrollmentBadge(kind: .prereq, text: .localized(.enrollmentBadgePrereq))
                EnrollmentBadge(kind: .waitlist, text: .localized(.enrollmentWaitlistPosition(7)))
                EnrollmentBadge(kind: .selected, text: "T01")
            }
            EnrollmentBanner(tone: .danger, title: .localized(.enrollmentConflictTitle)) {
                Text("Choca com TEC502 T01 na segunda. Troque uma das turmas.")
            }
            EnrollmentBanner(tone: .warn, title: .localized(.enrollmentClassFullTitle)) {
                Text("Entre na fila de espera — 6 na frente.")
            }
            EnrollmentBanner(tone: .info, title: .localized(.enrollmentNoConflictsTitle)) {
                Text(.enrollmentTimetableNoConflictBody(4))
            }
            HStack(spacing: 20) {
                EnrollmentSeatMeter(seats: EnrollmentSeats(filled: 31, total: 50))
                EnrollmentSeatMeter(seats: EnrollmentSeats(filled: 27, total: 30))
                EnrollmentSeatMeter(seats: EnrollmentSeats(filled: 45, total: 45))
            }
            ForEach([EnrollmentDiscipline].previewCatalogue.prefix(2)) { discipline in
                EnrollmentScheduleLines(section: discipline.sections[0])
            }
        }
        .padding(20)
    }
    .background(UNESColor.surface)
}
