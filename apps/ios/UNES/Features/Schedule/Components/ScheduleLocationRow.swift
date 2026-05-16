import SwiftUI

/// One-line location display, shared by the compact day cards. Handles four
/// cases from the design: none / online-only / prose modulo (long descriptive
/// building name as a multi-line headline) / one or two short tokens grouped
/// with a pin.
struct ScheduleLocationRow: View {
    enum Style { case normal(dim: Bool), inverted }

    let cls: ScheduleClass
    var style: Style = .normal(dim: false)

    private enum Kind { case modulo, room, campus, pin }
    private struct Part { let kind: Kind; let value: String }

    private var parts: [Part] {
        var out: [Part] = []
        if let m = cls.modulo { out.append(.init(kind: .modulo, value: m)) }
        if let r = cls.room   { out.append(.init(kind: .room,   value: r)) }
        return out
    }

    // Upstream stores a building/module label that's sometimes a short code
    // ("MT", "PV", "Módulo 5") and sometimes descriptive prose ("Pavilhão
    // de aula padrão 2° andar"). When it's prose we promote it to a
    // multi-line headline and demote room to a small meta row beneath.
    // Whitespace alone isn't a prose signal — "Módulo 5" should still
    // render in CODE mode.
    private var isProseModulo: Bool {
        guard let m = cls.modulo, !m.isEmpty else { return false }
        return m.count > 11
    }

    var body: some View {
        switch style {
        case .normal(let dim):
            normalBody.opacity(dim ? 0.5 : 1)
        case .inverted:
            invertedBody
        }
    }

    // MARK: Normal

    @ViewBuilder
    private var normalBody: some View {
        if parts.isEmpty {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.circle")
                    .font(.system(size: 11, weight: .medium))
                    .opacity(0.8)
                Text("Local não divulgado")
                    .font(UNESFont.sans(12))
                    .italic()
            }
            .foregroundStyle(UNESColor.ink3)
        } else if cls.campus == "Online" && cls.modulo == nil && cls.room == nil {
            HStack(spacing: 6) {
                icon(.campus)
                Text("ONLINE")
                    .font(UNESFont.mono(11, weight: .medium))
                    .tracking(0.44)
            }
            .foregroundStyle(UNESColor.ink)
        } else if isProseModulo, let modulo = cls.modulo {
            proseStack(
                headline: modulo,
                headlineColor: UNESColor.ink,
                metaColor: UNESColor.ink3
            )
        } else if parts.count == 1 {
            HStack(spacing: 6) {
                icon(parts[0].kind)
                Text(parts[0].value.uppercased())
                    .font(UNESFont.mono(11, weight: .medium))
                    .tracking(0.44)
            }
            .foregroundStyle(UNESColor.ink)
        } else {
            HStack(spacing: 6) {
                icon(.pin)
                ForEach(Array(parts.enumerated()), id: \.offset) { i, p in
                    if i > 0 {
                        Text("·").foregroundStyle(UNESColor.ink.opacity(0.35))
                    }
                    Text(p.value.uppercased())
                        .font(UNESFont.mono(11, weight: p.kind == .room ? .semibold : .medium))
                        .tracking(0.44)
                }
            }
            .foregroundStyle(UNESColor.ink)
            .lineLimit(1)
        }
    }

    // MARK: Inverted — used when the parent card is filled with the brand color.

    @ViewBuilder
    private var invertedBody: some View {
        if parts.isEmpty {
            Text("Local não divulgado")
                .font(UNESFont.sans(12))
                .italic()
                .foregroundStyle(Color.white.opacity(0.75))
        } else if isProseModulo, let modulo = cls.modulo {
            proseStack(
                headline: modulo,
                headlineColor: UNESColor.surfaceLight,
                metaColor: Color.white.opacity(0.78)
            )
        } else {
            HStack(spacing: 6) {
                Image(systemName: "mappin")
                    .font(.system(size: 10, weight: .medium))
                    .opacity(0.75)
                ForEach(Array(parts.enumerated()), id: \.offset) { i, p in
                    if i > 0 {
                        Text("·").foregroundStyle(Color.white.opacity(0.5))
                    }
                    Text(p.value.uppercased())
                        .font(UNESFont.mono(11, weight: i == parts.count - 1 ? .semibold : .medium))
                        .tracking(0.44)
                }
            }
            .foregroundStyle(UNESColor.surfaceLight)
            .lineLimit(1)
        }
    }

    // PROSE mode: long descriptive modulo (building) → 2-line sans headline +
    // a small uppercase room code beneath. The meta row indents 17pt so it
    // aligns under the prose text rather than the pin icon.
    @ViewBuilder
    private func proseStack(
        headline: String,
        headlineColor: Color,
        metaColor: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(alignment: .top, spacing: 6) {
                Image(systemName: "mappin")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(headlineColor.opacity(0.75))
                    .padding(.top, 1)
                Text(headline)
                    .font(UNESFont.sans(12.5, weight: .medium))
                    .lineSpacing(1)
                    .foregroundStyle(headlineColor)
                    .lineLimit(2)
                    .truncationMode(.tail)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if let room = cls.room {
                Text(room.uppercased())
                    .font(UNESFont.mono(9.5, weight: .semibold))
                    .tracking(0.95)
                    .foregroundStyle(metaColor)
                    .padding(.leading, 17)
            }
        }
    }

    private func icon(_ kind: Kind) -> some View {
        let name: String
        switch kind {
        case .modulo: name = "building"
        case .room:   name = "rectangle.portrait"
        case .campus: name = "globe"
        case .pin:    name = "mappin"
        }
        return Image(systemName: name)
            .font(.system(size: 10, weight: .medium))
            .opacity(0.7)
    }
}
