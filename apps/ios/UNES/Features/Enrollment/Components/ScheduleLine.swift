import SwiftUI

// UNES — compact weekly schedule summary for a section. Collapses identical
// times across days ("Seg, Qua · 13:30–15:30"). Ported from `ScheduleLine` in
// `screens-matricula-ui.jsx`.
struct ScheduleLine: View {
    let section: ClassSection

    /// Slots grouped by their "start–end" window, in first-seen order, with the
    /// weekdays they fall on (ascending).
    private var lines: [(time: String, days: [Int])] {
        var order: [String] = []
        var byTime: [String: [Int]] = [:]
        for slot in EnrollmentScheduling.slots(section) {
            let key = "\(slot.start)–\(slot.end)"
            if byTime[key] == nil { order.append(key) }
            byTime[key, default: []].append(slot.day)
        }
        return order.map { (time: $0, days: byTime[$0]!.sorted()) }
    }

    var body: some View {
        if lines.isEmpty {
            HStack(spacing: 6) {
                Image(systemName: "clock")
                    .font(.system(size: 11, weight: .regular))
                Text("Horário a definir")
                    .font(UNESFont.mono(10.5))
            }
            .foregroundStyle(UNESColor.ink4)
        } else {
            VStack(alignment: .leading, spacing: 3) {
                ForEach(lines, id: \.time) { line in
                    HStack(spacing: 6) {
                        Text(line.days.map { EnrollmentScheduling.daysShort[$0] }.joined(separator: ", "))
                            .font(UNESFont.mono(10.5, weight: .semibold))
                            .foregroundStyle(UNESColor.ink2)
                        Text("·")
                            .foregroundStyle(UNESColor.ink4)
                            .opacity(0.4)
                        Text(line.time)
                            .font(UNESFont.mono(10.5))
                            .foregroundStyle(UNESColor.ink3)
                    }
                }
            }
        }
    }
}
