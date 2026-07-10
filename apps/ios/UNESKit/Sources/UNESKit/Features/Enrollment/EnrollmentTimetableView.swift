import ComposableArchitecture
import SwiftUI

/// The weekly grade preview: conflict summary, the Monday–Saturday grid
/// with clashes burning red, and the picked-sections legend.
struct EnrollmentTimetableView: View {
    @Bindable var store: StoreOf<EnrollmentTimetableFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            EnrollmentAmbientWash(variant: .cool, opacity: 0.22)
            content
        }
        .toolbar {
            ToolbarItem(placement: .principalCompat) {
                Text(.enrollmentTimetableTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .hiddenTabBar()
        .safeAreaInset(edge: .bottom) {
            EnrollmentDock(
                session: store.session,
                primaryLabel: .localized(.enrollmentActionReview),
                onPrimary: { store.send(.reviewTapped) }
            )
        }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    summaryBanner
                        .fadeUp(delay: 0.08)
                        .padding(.bottom, 14)

                    EnrollmentTimetableGrid(picks: store.scheduledPicks)
                        .fadeUp(delay: 0.14)
                        .padding(.bottom, 14)

                    if store.pendingScheduleCount > 0 {
                        pendingNote
                            .fadeUp(delay: 0.18)
                            .padding(.bottom, 14)
                    }

                    if !store.session.resolvedPicks.isEmpty {
                        legend
                            .fadeUp(delay: 0.22)
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(.enrollmentTimetableTitle)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(subtitle)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    private var subtitle: String {
        let scheduled = store.scheduledPicks.count
        if store.session.picks.isEmpty { return .localized(.enrollmentTimetableNoneSelected) }
        return .localized(.enrollmentTimetableScheduledCount(scheduled))
    }

    @ViewBuilder
    private var summaryBanner: some View {
        let conflicts = store.session.conflicts
        if !conflicts.isEmpty {
            EnrollmentBanner(tone: .danger, title: String.localized(.enrollmentTimetableConflictsTitle(conflicts.count))) {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(Array(conflicts.enumerated()), id: \.offset) { _, conflict in
                        Text(verbatim: "\(conflict.aCode) \(conflict.aLabel) × \(conflict.bCode) \(conflict.bLabel) · \(EnrollmentFormat.dayFull(conflict.day))")
                    }
                }
            }
        } else if store.session.picks.isEmpty {
            EnrollmentBanner(tone: .neutral, title: String.localized(.enrollmentTimetableEmptyTitle)) {
                Text(.enrollmentTimetableEmptyBody)
            }
        } else {
            EnrollmentBanner(tone: .info, title: String.localized(.enrollmentNoConflictsTitle)) {
                // Counts every pick — "suas 0 turmas" would read nonsense when
                // everything picked is still "a definir".
                Text(.enrollmentTimetableNoConflictBody(store.session.resolvedPicks.count))
            }
        }
    }

    private var pendingNote: some View {
        let count = store.pendingScheduleCount
        return HStack(spacing: 8) {
            Circle()
                .fill(UNESColor.ink4)
                .frame(width: 6, height: 6)
            Text(.enrollmentTimetablePendingNote(count))
                .font(.system(size: 12.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var legend: some View {
        FlowLayout(spacing: 8) {
            ForEach(store.session.resolvedPicks) { pick in
                HStack(spacing: 7) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(pick.discipline.tint)
                        .frame(width: 9, height: 9)
                    Text(pick.discipline.code)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(UNESColor.ink2)
                    Text(pick.section.label)
                        .font(.system(size: 10.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                .padding(EdgeInsets(top: 6, leading: 11, bottom: 6, trailing: 11))
                .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(UNESColor.cardLine)
                }
            }
        }
    }

}

// MARK: - Grid

/// The 07:00–23:00 × Mon–Sat grid. Blocks are laid into per-day lanes so
/// overlaps sit side by side; clashing blocks flip to the danger tone.
struct EnrollmentTimetableGrid: View {
    var picks: [EnrollmentResolvedPick]
    var height: CGFloat = 560

    @Environment(\.colorScheme) private var colorScheme

    private var pointsPerMinute: CGFloat {
        height / CGFloat(EnrollmentTimetableLayout.endMinute - EnrollmentTimetableLayout.startMinute)
    }

    private var hours: [Int] {
        Array(stride(from: 7, through: 23, by: 2))
    }

    var body: some View {
        let columns = EnrollmentTimetableLayout.columns(for: picks)

        VStack(spacing: 8) {
            dayHeader
            HStack(alignment: .top, spacing: 3) {
                hourRail
                ForEach(EnrollmentTimetableLayout.days, id: \.self) { day in
                    dayColumn(columns[day] ?? EnrollmentTimetableColumn())
                }
            }
        }
        .padding(EdgeInsets(top: 14, leading: 6, bottom: 12, trailing: 12))
        .enrollmentCard(radius: 22)
    }

    private var dayHeader: some View {
        HStack(spacing: 3) {
            Color.clear.frame(width: 30, height: 1)
            ForEach(EnrollmentTimetableLayout.days, id: \.self) { day in
                Text(EnrollmentFormat.dayShort(day))
                    .font(.system(size: 10.5, weight: .semibold))
                    .tracking(0.2)
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var hourRail: some View {
        ZStack(alignment: .topTrailing) {
            Color.clear
            ForEach(hours, id: \.self) { hour in
                Text(String(format: "%02d", hour))
                    .font(.system(size: 9, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.trailing, 3)
                    .offset(y: offset(minute: hour * 60) - 5)
            }
        }
        .frame(width: 30, height: height)
    }

    private func dayColumn(_ column: EnrollmentTimetableColumn) -> some View {
        GeometryReader { geometry in
            let laneWidth = (geometry.size.width - 2) / CGFloat(column.lanes)

            ZStack(alignment: .topLeading) {
                ForEach(hours, id: \.self) { hour in
                    Rectangle()
                        .fill(UNESColor.line.opacity(0.5))
                        .frame(height: 0.5)
                        .offset(y: offset(minute: hour * 60))
                }

                ForEach(column.blocks) { block in
                    blockView(block, width: laneWidth)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: height)
        .overlay(alignment: .leading) {
            Rectangle().fill(UNESColor.line).frame(width: 0.5)
        }
    }

    private func blockView(_ block: EnrollmentTimetableBlock, width: CGFloat) -> some View {
        let tint = UNESColor.disciplineReadableColor(block.colorIndex)
        let text = block.conflicting ? EnrollmentTone.danger : tint
        let blockHeight = max(16, CGFloat(block.endMinute - block.startMinute) * pointsPerMinute - 2)
        let showsTime = blockHeight > 30

        return VStack(alignment: .leading, spacing: 1) {
            Text(block.code)
                .font(.system(size: 8.5, weight: .bold))
                .foregroundStyle(text)
                .lineLimit(1)
            if showsTime {
                Text(ScheduleFormat.timeLabel(block.startMinute))
                    .font(.system(size: 7.5, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(text.opacity(0.75))
            }
        }
        .padding(EdgeInsets(top: 3, leading: 4, bottom: 3, trailing: 2))
        .frame(width: max(0, width - 2), height: blockHeight, alignment: .topLeading)
        .background(fill(block, tint: tint), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
        .overlay(alignment: .leading) {
            UnevenRoundedRectangle(topLeadingRadius: 7, bottomLeadingRadius: 7)
                .fill(block.conflicting ? EnrollmentTone.danger : tint)
                .frame(width: 2.5)
        }
        .overlay {
            if block.conflicting {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .strokeBorder(EnrollmentTone.danger, lineWidth: 1.5)
            }
        }
        .offset(
            x: CGFloat(block.lane) * width + 1,
            y: offset(minute: block.startMinute) + 1
        )
    }

    private func fill(_ block: EnrollmentTimetableBlock, tint: Color) -> Color {
        if block.conflicting {
            return EnrollmentTone.danger.opacity(colorScheme == .dark ? 0.28 : 0.14)
        }
        return tint.opacity(colorScheme == .dark ? 0.3 : 0.18)
    }

    private func offset(minute: Int) -> CGFloat {
        CGFloat(minute - EnrollmentTimetableLayout.startMinute) * pointsPerMinute
    }
}

#Preview("Com conflito") {
    var session = EnrollmentSession.preview
    session.picks[1].sectionId = 30401
    return NavigationStack {
        EnrollmentTimetableView(
            store: Store(initialState: EnrollmentTimetableFeature.State(session: session)) {
                EnrollmentTimetableFeature()
            }
        )
    }
}

#Preview("Sem conflitos") {
    NavigationStack {
        EnrollmentTimetableView(
            store: Store(initialState: EnrollmentTimetableFeature.State(session: .preview)) {
                EnrollmentTimetableFeature()
            }
        )
    }
}
