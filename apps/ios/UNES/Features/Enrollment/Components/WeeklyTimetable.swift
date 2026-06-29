import SwiftUI

// UNES — the enrollment centerpiece: a Mon–Sat × 07:00–23:00 timetable that
// lays the picked sections onto a real time axis, splits overlapping blocks
// into lanes, and flags clashes in red. Ported from `Timetable` in
// `screens-matricula-ui.jsx`.

/// One discipline+section pair to render on the grid.
struct TimetableItem: Identifiable {
    let discipline: OfferedDiscipline
    let section: ClassSection
    var id: Int64 { section.id }
}

struct WeeklyTimetable: View {
    let items: [TimetableItem]
    var height: CGFloat = 560

    @Environment(\.colorScheme) private var scheme

    private static let days = [1, 2, 3, 4, 5, 6]
    private static let axisStart = 7 * 60      // 07:00
    private static let axisEnd = 23 * 60       // 23:00
    private static let hours = stride(from: 7, through: 23, by: 2).map { $0 }
    private let railWidth: CGFloat = 32
    private let gap: CGFloat = 3

    private var span: CGFloat { CGFloat(Self.axisEnd - Self.axisStart) }
    private var ppm: CGFloat { height / span }

    // MARK: Layout model

    private struct Block: Identifiable {
        let id: String
        let disciplineId: Int64
        let day: Int
        let startMin: Int
        let endMin: Int
        let code: String
        let tone: EnrollmentTone
        var lane: Int = 0
        var conflict: Bool = false
    }

    private struct DayLayout {
        var lanes: Int
        var blocks: [Block]
    }

    private var layout: [Int: DayLayout] {
        var byDay: [Int: [Block]] = [:]
        for item in items {
            for slot in EnrollmentScheduling.slots(item.section) {
                let block = Block(
                    id: "\(item.discipline.id)-\(item.section.id)-\(slot.day)-\(slot.start)",
                    disciplineId: item.discipline.id,
                    day: slot.day,
                    startMin: EnrollmentScheduling.toMinutes(slot.start),
                    endMin: EnrollmentScheduling.toMinutes(slot.end),
                    code: item.discipline.code,
                    tone: item.section.tone
                )
                byDay[slot.day, default: []].append(block)
            }
        }
        return byDay.mapValues(Self.layoutDay)
    }

    /// Greedy lane packing + pairwise clash flagging within a single day.
    private static func layoutDay(_ blocks: [Block]) -> DayLayout {
        var sorted = blocks.sorted { $0.startMin < $1.startMin }
        var laneEnds: [Int] = []
        for i in sorted.indices {
            var placed = false
            for lane in laneEnds.indices where sorted[i].startMin >= laneEnds[lane] {
                sorted[i].lane = lane
                laneEnds[lane] = sorted[i].endMin
                placed = true
                break
            }
            if !placed {
                sorted[i].lane = laneEnds.count
                laneEnds.append(sorted[i].endMin)
            }
        }
        for i in sorted.indices {
            for j in sorted.indices where i != j {
                guard sorted[i].disciplineId != sorted[j].disciplineId else { continue }
                if sorted[i].startMin < sorted[j].endMin && sorted[j].startMin < sorted[i].endMin {
                    sorted[i].conflict = true
                }
            }
        }
        return DayLayout(lanes: Swift.max(1, laneEnds.count), blocks: sorted)
    }

    // MARK: Body

    var body: some View {
        VStack(spacing: 8) {
            header
            grid
        }
        .padding(EdgeInsets(top: 12, leading: 8, bottom: 12, trailing: 12))
        .cardSurface(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var header: some View {
        HStack(spacing: gap) {
            Color.clear.frame(width: railWidth)
            ForEach(Self.days, id: \.self) { day in
                Text(EnrollmentScheduling.daysShort[day].uppercased())
                    .font(UNESFont.mono(9.5, weight: .semibold))
                    .tracking(0.76)
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.leading, 2)
    }

    private var grid: some View {
        GeometryReader { geo in
            let w = geo.size.width
            // Clamp to non-negative: during the first layout pass `w` can be 0,
            // which would otherwise drive `colW` (and the lane widths below)
            // negative and trip "invalid frame dimension".
            let colW = max(0, (w - railWidth - gap * 6) / 6)
            let dayLayouts = layout

            ZStack(alignment: .topLeading) {
                gridlines(width: w)
                separators(colW: colW)
                timeRail
                ForEach(Self.days, id: \.self) { day in
                    let dl = dayLayouts[day] ?? DayLayout(lanes: 1, blocks: [])
                    ForEach(dl.blocks) { block in
                        blockView(block, lanes: dl.lanes, colW: colW)
                    }
                }
            }
            .frame(width: w, height: height, alignment: .topLeading)
        }
        .frame(height: height)
    }

    // MARK: Pieces

    private func y(forMinute minute: Int) -> CGFloat {
        CGFloat(minute - Self.axisStart) * ppm
    }

    private func colX(_ dayZeroBased: Int, colW: CGFloat) -> CGFloat {
        railWidth + gap + CGFloat(dayZeroBased) * (colW + gap)
    }

    private func gridlines(width: CGFloat) -> some View {
        ForEach(Self.hours, id: \.self) { h in
            DashedHairline()
                .stroke(UNESColor.line, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                .frame(width: width - railWidth, height: 1)
                .offset(x: railWidth, y: y(forMinute: h * 60))
        }
    }

    private func separators(colW: CGFloat) -> some View {
        ForEach(Array(Self.days.indices), id: \.self) { idx in
            Rectangle()
                .fill(UNESColor.line)
                .frame(width: 1, height: height)
                .offset(x: colX(idx, colW: colW))
        }
    }

    private var timeRail: some View {
        ForEach(Self.hours, id: \.self) { h in
            Text(String(format: "%02dh", h))
                .font(UNESFont.mono(8.5))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: railWidth - 4, alignment: .trailing)
                .offset(y: y(forMinute: h * 60) - 6)
        }
    }

    @ViewBuilder
    private func blockView(_ block: Block, lanes: Int, colW: CGFloat) -> some View {
        let laneW = colW / CGFloat(Swift.max(1, lanes))
        let top = y(forMinute: block.startMin) + 1
        let h = Swift.max(16, CGFloat(block.endMin - block.startMin) * ppm - 2)
        let x = colX(block.day - 1, colW: colW) + CGFloat(block.lane) * laneW + 1
        // Many overlapping lanes can make `laneW` < 2; keep the block width
        // non-negative so a crowded day never trips an invalid frame dimension.
        let blockW = Swift.max(1, laneW - 2)
        let accent = block.conflict ? EnrollmentPalette.danger : block.tone.color

        VStack(alignment: .leading, spacing: 0) {
            Text(block.code)
                .font(UNESFont.mono(8, weight: .bold))
                .foregroundStyle(blockText(block))
                .lineLimit(1)
            if h > 30 {
                Text(timeLabel(block.startMin))
                    .font(UNESFont.mono(7))
                    .foregroundStyle(blockText(block).opacity(0.7))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(.leading, 4)
        .padding(.trailing, 3)
        .padding(.top, 2)
        .frame(width: blockW, height: h, alignment: .topLeading)
        .background(blockFill(block))
        .overlay(alignment: .leading) {
            Rectangle().fill(accent).frame(width: 2.5)
        }
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .strokeBorder(
                    block.conflict ? EnrollmentPalette.danger : block.tone.color.mix(with: UNESColor.surface, by: 0.55, in: scheme),
                    lineWidth: 1
                )
        )
        .overlay(alignment: .topTrailing) {
            if block.conflict {
                Text("!")
                    .font(UNESFont.sans(10, weight: .heavy))
                    .foregroundStyle(EnrollmentPalette.danger)
                    .padding(.trailing, 2)
                    .padding(.top, 1)
            }
        }
        .offset(x: x, y: top)
    }

    private func blockFill(_ block: Block) -> Color {
        if block.conflict {
            return EnrollmentPalette.danger.opacity(scheme == .dark ? 0.25 : 0.13)
        }
        return block.tone.color.mix(with: UNESColor.surface, by: scheme == .dark ? 0.40 : 0.32, in: scheme)
    }

    private func blockText(_ block: Block) -> Color {
        if block.conflict { return EnrollmentPalette.danger }
        if scheme == .dark {
            let warmWhite = Color(red: 0xF5 / 255, green: 0xEF / 255, blue: 0xE6 / 255)
            return block.tone.color.mix(with: warmWhite, by: 0.6, in: scheme)
        }
        return block.tone.color
    }

    private func timeLabel(_ minute: Int) -> String {
        String(format: "%02d:%02d", minute / 60, minute % 60)
    }
}

/// A single horizontal hairline used for the hour gridlines.
private struct DashedHairline: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.minX, y: rect.midY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return p
    }
}

#Preview {
    WeeklyTimetable(
        items: EnrollmentState.previewSeeded.picks
            .filter { EnrollmentScheduling.hasSchedule($0.section) }
            .map { TimetableItem(discipline: $0.discipline, section: $0.section) }
    )
    .padding()
    .background(UNESColor.surface)
}
