import SwiftUI

/// One-line location display, shared by the compact day cards. Handles four
/// cases from the design: none / online-only / single part / 2–3 parts grouped
/// with optional campus tail.
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
        if let c = cls.campus { out.append(.init(kind: .campus, value: c)) }
        return out
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
        } else if parts.count == 1 {
            HStack(spacing: 6) {
                icon(parts[0].kind)
                Text(parts[0].value.uppercased())
                    .font(UNESFont.mono(11, weight: .medium))
                    .tracking(0.44)
            }
            .foregroundStyle(UNESColor.ink)
        } else {
            let physical = parts.filter { $0.kind != .campus }
            let campus = parts.first { $0.kind == .campus }
            HStack(spacing: 6) {
                icon(.pin)
                ForEach(Array(physical.enumerated()), id: \.offset) { i, p in
                    if i > 0 {
                        Text("·").foregroundStyle(UNESColor.ink.opacity(0.35))
                    }
                    Text(p.value.uppercased())
                        .font(UNESFont.mono(11, weight: p.kind == .room ? .semibold : .medium))
                        .tracking(0.44)
                }
                if let c = campus {
                    Text(c.value)
                        .font(UNESFont.mono(9.5, weight: .medium))
                        .tracking(0.76)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            RoundedRectangle(cornerRadius: 5, style: .continuous)
                                .fill(UNESColor.surface2)
                        )
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.leading, 2)
                }
            }
            .foregroundStyle(UNESColor.ink)
            .lineLimit(1)
        }
    }

    // MARK: Inverted — used when the parent card is filled with the brand color.

    @ViewBuilder
    private var invertedBody: some View {
        let physical = parts.filter { $0.kind != .campus }.map(\.value)
        if physical.isEmpty && cls.campus == nil {
            Text("Local não divulgado")
                .font(UNESFont.sans(12))
                .italic()
                .foregroundStyle(Color.white.opacity(0.75))
        } else {
            HStack(spacing: 6) {
                Image(systemName: "mappin")
                    .font(.system(size: 10, weight: .medium))
                    .opacity(0.75)
                ForEach(Array(physical.enumerated()), id: \.offset) { i, val in
                    if i > 0 {
                        Text("·").foregroundStyle(Color.white.opacity(0.5))
                    }
                    Text(val.uppercased())
                        .font(UNESFont.mono(11, weight: i == physical.count - 1 ? .semibold : .medium))
                        .tracking(0.44)
                }
                if let c = cls.campus {
                    Text(c)
                        .font(UNESFont.mono(9.5, weight: .medium))
                        .tracking(0.76)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            RoundedRectangle(cornerRadius: 5, style: .continuous)
                                .fill(Color.white.opacity(0.2))
                        )
                        .padding(.leading, 2)
                }
            }
            .foregroundStyle(UNESColor.surfaceLight)
            .lineLimit(1)
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
