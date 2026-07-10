import SwiftUI

/// The class-location display shared by the day cards and the status hero.
/// Four modes: unknown / online-only / prose building headline / short
/// location codes with a campus chip.
struct ScheduleLocationRow: View {
    let cls: ScheduleClass
    var inverted = false
    var dim = false

    var body: some View {
        content
            .opacity(dim ? 0.55 : 1)
    }

    // Upstream's building label is sometimes a short code ("MT", "PV",
    // "Módulo 5") and sometimes descriptive prose ("Pavilhão de aula padrão
    // 2° andar"). Prose gets promoted to a multi-line headline with the room
    // demoted to a meta row beneath. Whitespace alone isn't a prose signal —
    // "Módulo 5" must still render in code mode.
    private var proseModulo: String? {
        guard let modulo = cls.modulo, modulo.count > 11 else { return nil }
        return modulo
    }

    @ViewBuilder
    private var content: some View {
        if cls.modulo == nil, cls.room == nil, cls.campus == nil {
            HStack(spacing: 6) {
                icon("exclamationmark.circle")
                Text(.scheduleLocationUnset)
                    .font(.system(size: 13, weight: .medium))
            }
            .foregroundStyle(ink3)
        } else if cls.isOnline {
            HStack(spacing: 6) {
                icon("globe")
                Text(.scheduleLocationOnline)
                    .font(.system(size: 13, weight: .semibold))
            }
            .foregroundStyle(ink)
        } else if let prose = proseModulo {
            proseStack(prose)
        } else {
            codeRow
        }
    }

    // MARK: Prose — building name as a 2-line headline, room + campus beneath

    private func proseStack(_ prose: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(alignment: .top, spacing: 6) {
                icon("mappin")
                    .padding(.top, 1)
                Text(prose)
                    .font(.system(size: 13, weight: .medium))
                    .lineSpacing(2)
                    .lineLimit(2)
                    .foregroundStyle(ink)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if cls.room != nil || cls.campus != nil {
                HStack(spacing: 6) {
                    if let room = cls.room {
                        Text(room)
                            .font(.system(size: 11, weight: .semibold))
                            .monospacedDigit()
                            .foregroundStyle(ink3)
                    }
                    if cls.room != nil, cls.campus != nil {
                        dot
                    }
                    if let campus = cls.campus {
                        chip(campus)
                    }
                }
                .padding(.leading, 18)
            }
        }
    }

    // MARK: Code — short module/room tokens grouped behind one icon

    private var codeRow: some View {
        let parts = [cls.modulo, cls.room].compactMap(\.self)
        return HStack(spacing: 6) {
            icon(parts.count > 1 ? "mappin" : "door.left.hand.closed")
            ForEach(Array(parts.enumerated()), id: \.offset) { index, part in
                if index > 0 {
                    dot
                }
                Text(part)
                    .font(.system(size: 13, weight: index == parts.count - 1 ? .semibold : .medium))
            }
            if let campus = cls.campus {
                chip(campus)
            }
        }
        .foregroundStyle(ink)
        .lineLimit(1)
    }

    // MARK: Pieces

    private func icon(_ name: String) -> some View {
        Image(systemName: name)
            .font(.system(size: 11, weight: .medium))
            .opacity(0.65)
    }

    private var dot: some View {
        Text(verbatim: "·").opacity(0.4)
    }

    private func chip(_ label: String) -> some View {
        Text(label)
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(chipFg)
            .padding(EdgeInsets(top: 1, leading: 6, bottom: 1, trailing: 6))
            .background(chipBg, in: RoundedRectangle(cornerRadius: 5, style: .continuous))
    }

    private var ink: Color { inverted ? .white : UNESColor.ink }
    private var ink3: Color { inverted ? .white.opacity(0.72) : UNESColor.ink3 }
    private var chipBg: Color { inverted ? .white.opacity(0.18) : UNESColor.surface2 }
    private var chipFg: Color { inverted ? .white : UNESColor.ink3 }
}

#Preview {
    let day = ScheduleOverview.preview().days
    VStack(alignment: .leading, spacing: 16) {
        ScheduleLocationRow(cls: day[3].classes[0])
        ScheduleLocationRow(cls: day[3].classes[1])
        ScheduleLocationRow(cls: day[2].classes[2])
        ScheduleLocationRow(cls: day[1].classes[2])
        ScheduleLocationRow(cls: day[4].classes[0])
        ScheduleLocationRow(cls: day[3].classes[0], inverted: true)
            .padding(12)
            .background(UNESColor.plum, in: RoundedRectangle(cornerRadius: 12))
    }
    .padding(24)
    .background(UNESColor.surface)
}
