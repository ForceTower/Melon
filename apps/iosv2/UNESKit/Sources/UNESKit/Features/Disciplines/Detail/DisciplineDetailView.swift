import ComposableArchitecture
import SwiftUI

/// One discipline, end to end: mesh hero with the partial mean, stat cards,
/// grades, attendance, syllabus, the lecture timeline, and attachments. The
/// inline nav title fades in as the large header scrolls away, mirroring the
/// system large-title collapse.
struct DisciplineDetailView: View {
    @Bindable var store: StoreOf<DisciplineDetailFeature>
    @State private var titleProgress: CGFloat = 0

    private var color: Color { UNESColor.disciplineColor(store.colorIndex) }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientTint

            if let detail = store.detail {
                content(detail)
            } else {
                SpinnerRing(size: 28, color: UNESColor.accent, trackColor: UNESColor.surface3)
                    .frame(maxHeight: .infinity)
            }
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(store.name)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .lineLimit(1)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
    }

    // MARK: Content

    private func content(_ detail: DisciplineDetail) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                header(detail)
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    DisciplineDetailHero(detail: detail, color: color, selectedGroup: store.selectedGroup)
                        .scaleIn(delay: 0.08, duration: 0.62)
                        .padding(.bottom, 22)

                    statRow(detail)
                        .fadeUp(delay: 0.16)
                        .padding(.bottom, 22)

                    DisciplineGradesBlock(detail: detail, color: color, selectedGroup: store.selectedGroup)
                        .fadeUp(delay: 0.24)
                        .padding(.bottom, 22)

                    DisciplinePresencaCard(detail: detail)
                        .fadeUp(delay: 0.3)
                        .padding(.bottom, 22)

                    if detail.ementa != nil {
                        DisciplineEmentaCard(detail: detail, color: color)
                            .fadeUp(delay: 0.36)
                            .padding(.bottom, 22)
                    }

                    DisciplineClassesTimeline(detail: detail, color: color, selectedGroup: store.selectedGroup)
                        .fadeUp(delay: 0.42)
                        .padding(.bottom, 22)

                    if !detail.attachments.isEmpty {
                        DisciplineAttachmentsBlock(detail: detail, color: color, selectedGroup: store.selectedGroup)
                            .fadeUp(delay: 0.48)
                            .padding(.bottom, 22)
                    }

                    footer(detail)
                        .fadeUp(delay: 0.54)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 16)
            .padding(.bottom, 12)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 60) / 44, 0), 1)
        }
    }

    // MARK: Large header

    private func header(_ detail: DisciplineDetail) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Text(detail.code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.5)
                    .foregroundStyle(color)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 7))
                if let department = detail.department {
                    Text(DisciplinesFormat.departmentLabel(department))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
            }
            .padding(.bottom, 10)

            Text(detail.name)
                .font(.system(size: 34, weight: .bold))
                .tracking(-1.19)
                .lineSpacing(1)
                .foregroundStyle(UNESColor.ink)

            teachers(detail)
                .padding(.top, 12)

            if detail.hasMultipleGroups {
                DisciplineGroupSegmented(
                    groups: detail.groups,
                    selected: store.selectedGroup,
                    accent: color
                ) { code in
                    store.send(.groupSelected(code))
                }
                .padding(.top, 16)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 6, trailing: 20))
    }

    /// One row per group when browsing "Tudo" on a multi-group discipline;
    /// otherwise the single (or selected) professor.
    @ViewBuilder
    private func teachers(_ detail: DisciplineDetail) -> some View {
        if detail.hasMultipleGroups, store.selectedGroup == nil {
            VStack(alignment: .leading, spacing: 5) {
                ForEach(detail.groups) { group in
                    teacherRow(name: group.teacherName, groupCode: group.code, kind: group.kind)
                }
            }
        } else {
            let group = detail.groups.first { $0.code == store.selectedGroup }
            teacherRow(
                name: group?.teacherName ?? detail.teacherName,
                groupCode: detail.hasMultipleGroups ? group?.code : nil,
                kind: nil
            )
        }
    }

    @ViewBuilder
    private func teacherRow(name: String?, groupCode: String?, kind: String?) -> some View {
        if name != nil || groupCode != nil {
            HStack(spacing: 7) {
                Image(systemName: "person")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                if let name {
                    Text(name)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(UNESColor.ink2)
                        .lineLimit(1)
                }
                if let groupCode {
                    Text(groupCode)
                        .font(.system(size: 9.5, weight: .semibold))
                        .tracking(0.3)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 6))
                }
                if let kind {
                    Text("· \(kind)")
                        .font(.system(size: 12.5))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
        }
    }

    // MARK: Stats

    private func statRow(_ detail: DisciplineDetail) -> some View {
        HStack(spacing: 10) {
            DisciplineStatCard(
                icon: "clock",
                tint: color,
                label: "Carga",
                value: "\(detail.hours)h",
                sub: "carga horária"
            )
            DisciplineStatCard(
                icon: "flame",
                tint: detail.absenceRisk == .ok ? UNESColor.ink3 : UNESColor.caution,
                label: "Faltas",
                value: "\(detail.missedHours)",
                sub: "\(max(0, detail.allowedMissedHours - detail.missedHours)) disponíveis",
                valueColor: absenceValueColor(detail.absenceRisk)
            )
        }
    }

    private func absenceValueColor(_ risk: AbsenceRisk) -> Color {
        switch risk {
        case .critical: UNESColor.coral
        case .warning: UNESColor.caution
        case .ok: UNESColor.ink
        }
    }

    // MARK: Footer

    private func footer(_ detail: DisciplineDetail) -> some View {
        TimelineView(.everyMinute) { context in
            if let syncedAt = detail.syncedAt {
                Text(HomeFormat.updatedLabel(lastRefreshed: syncedAt, now: context.date))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .frame(maxWidth: .infinity)
                    .padding(EdgeInsets(top: 4, leading: 24, bottom: 8, trailing: 24))
            }
        }
    }

    /// Ambient wash in the discipline's color bleeding down from the top.
    private var ambientTint: some View {
        EllipticalGradient(
            stops: [
                .init(color: color, location: 0),
                .init(color: .clear, location: 0.68),
            ],
            center: .top
        )
        .frame(height: 320)
        .padding(.horizontal, -50)
        .mask {
            LinearGradient(
                stops: [
                    .init(color: .white, location: 0),
                    .init(color: .clear, location: 0.94),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
        }
        .opacity(0.32)
        .offset(y: -60)
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

// MARK: - Group segmented control

/// "Tudo | Teórica T01 | Prática T01P01" — a two-line segmented control; the
/// native picker can't stack the group code under the kind.
struct DisciplineGroupSegmented: View {
    var groups: [DisciplineDetailGroup]
    var selected: String?
    var accent: Color
    var onSelect: (String?) -> Void

    var body: some View {
        HStack(spacing: 3) {
            segment(kind: "Tudo", code: nil)
            ForEach(groups) { group in
                segment(kind: group.kind ?? group.code ?? "Turma", code: group.code)
            }
        }
        .padding(3)
        .background(Color(hex: 0x787880, opacity: 0.16), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    private func segment(kind: String, code: String?) -> some View {
        let active = selected == code
        return Button {
            withAnimation(.easeOut(duration: 0.15)) {
                onSelect(code)
            }
        } label: {
            VStack(spacing: 1) {
                Text(kind)
                    .font(.system(size: 13, weight: .semibold))
                    .tracking(-0.13)
                    .foregroundStyle(active ? UNESColor.ink : UNESColor.ink3)
                if let code {
                    Text(code)
                        .font(.system(size: 9, weight: .semibold))
                        .tracking(0.3)
                        .monospacedDigit()
                        .foregroundStyle(active ? accent : UNESColor.ink4)
                }
            }
            .lineLimit(1)
            .padding(EdgeInsets(top: 6, leading: 8, bottom: 5, trailing: 8))
            .frame(maxWidth: .infinity)
            .background {
                if active {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(UNESColor.card)
                        .shadow(color: .black.opacity(0.14), radius: 1.5, y: 1)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

#Preview("Uma turma") {
    NavigationStack {
        DisciplineDetailView(
            store: Store(
                initialState: DisciplineDetailFeature.State(
                    summary: DisciplinesOverview.preview().current!.disciplines[0],
                    semesterId: "sem-2026-1"
                )
            ) {
                DisciplineDetailFeature()
            }
        )
    }
}

#Preview("Teórica + prática") {
    NavigationStack {
        DisciplineDetailView(
            store: Store(
                initialState: DisciplineDetailFeature.State(
                    summary: DisciplinesOverview.preview().current!.disciplines[2],
                    semesterId: "sem-2026-1"
                )
            ) {
                DisciplineDetailFeature()
            } withDependencies: {
                $0.disciplinesRepository.detail = { _, _, now in .previewMultiGroup(now: now) }
            }
        )
    }
}
