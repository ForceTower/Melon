import ComposableArchitecture
import SwiftUI

/// One discipline: prerequisites context, the turma cards, and the
/// "aceitar outra turma" preference once something is picked.
struct EnrollmentDisciplineView: View {
    @Bindable var store: StoreOf<EnrollmentDisciplineFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            EnrollmentAmbientWash(variant: .cool, opacity: 0.2)
            if let discipline = store.discipline {
                content(discipline)
            }
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text(store.discipline?.code ?? "")
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
                primaryLabel: .localized(.commonDone),
                onPrimary: { store.send(.doneTapped) },
                secondaryLabel: String.localized(.enrollmentActionGrid),
                onSecondary: { store.send(.timetableTapped) }
            )
        }
    }

    private func content(_ discipline: EnrollmentDiscipline) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                header(discipline)
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    if !discipline.prereqs.isEmpty {
                        prereqBanner(discipline)
                            .fadeUp(delay: 0.08)
                            .padding(.bottom, 16)
                    }

                    VStack(spacing: 0) {
                        EnrollmentSectionHeader(title: .enrollmentDisciplineChooseSection)
                        VStack(spacing: 12) {
                            ForEach(discipline.sections) { section in
                                EnrollmentSectionCard(
                                    discipline: discipline,
                                    section: section,
                                    isSelected: store.pick?.sectionId == section.id,
                                    clash: store.session.clash(with: section, excluding: discipline.id),
                                    useQueue: store.session.window?.useQueue ?? false
                                ) {
                                    store.send(.sectionTapped(section.id), animation: UNESMotion.ease(0.3))
                                }
                            }
                        }
                    }
                    .fadeUp(delay: 0.14)

                    if let pick = store.pick, let section = discipline.section(pick.sectionId) {
                        allowsOtherCard(discipline, section: section, pick: pick)
                            .padding(.top, 16)
                            .transition(.opacity.combined(with: .move(edge: .bottom)))
                    }
                }
                .padding(.horizontal, 16)
                .animation(UNESMotion.ease(0.3), value: store.pick?.sectionId)
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

    private func header(_ discipline: EnrollmentDiscipline) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(verbatim: "\(discipline.code) · \(String.localized(discipline.mandatory ? .enrollmentBadgeMandatory : .enrollmentBadgeOptional))")
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.48)
                .foregroundStyle(discipline.tint)
            Text(discipline.name)
                .font(.system(size: 34, weight: .bold))
                .tracking(-1.19)
                .foregroundStyle(UNESColor.ink)
                .fixedSize(horizontal: false, vertical: true)
            Text(verbatim: "\(discipline.workload)h · \(EnrollmentFormat.sectionCountLabel(discipline.sections.count))")
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink3)
            if discipline.suggestion {
                EnrollmentBadge(kind: .suggested, text: .localized(.enrollmentBadgeSuggestedByCourse))
                    .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 16, trailing: 20))
    }

    private func prereqBanner(_ discipline: EnrollmentDiscipline) -> some View {
        let unmet = discipline.hasUnmetPrereq
        return EnrollmentBanner(
            tone: unmet ? .danger : .info,
            title: String.localized(unmet ? .enrollmentPrereqUnmetTitle : .enrollmentPrereqMetTitle)
        ) {
            VStack(alignment: .leading, spacing: 4) {
                Text(prereqLine(discipline.prereqs))
                if unmet {
                    Text(.enrollmentPrereqUnmetNote)
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
    }

    private func prereqLine(_ prereqs: [EnrollmentPrerequisite]) -> AttributedString {
        var line = AttributedString()
        for (index, prereq) in prereqs.enumerated() {
            if index > 0 { line += AttributedString(", ") }
            var code = AttributedString(prereq.code)
            code.font = .system(size: 12.5, weight: .bold)
            line += code
            line += AttributedString(" \(prereq.name)")
            if !prereq.met {
                var pending = AttributedString(" — " + String.localized(.enrollmentPrereqPending))
                pending.foregroundColor = EnrollmentTone.danger
                line += pending
            }
        }
        return line
    }

    private func allowsOtherCard(_ discipline: EnrollmentDiscipline, section: EnrollmentSection, pick: EnrollmentPick) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(.enrollmentAllowOtherTitle)
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.14)
                    .foregroundStyle(UNESColor.ink)
                Text(.enrollmentAllowOtherBody(section.label, discipline.code))
                    .font(.system(size: 12.5, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink3)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle(String.localized(.enrollmentAllowOtherTitle), isOn: Binding(
                get: { pick.allowsOther },
                set: { store.send(.allowsOtherChanged($0)) }
            ))
            .labelsHidden()
        }
        .padding(EdgeInsets(top: 14, leading: 15, bottom: 14, trailing: 15))
        .enrollmentCard(radius: 18)
    }

}

#Preview("Turmas") {
    NavigationStack {
        EnrollmentDisciplineView(
            store: Store(initialState: EnrollmentDisciplineFeature.State(disciplineId: 203, session: .preview)) {
                EnrollmentDisciplineFeature()
            }
        )
    }
}

#Preview("Pré-requisito pendente") {
    NavigationStack {
        EnrollmentDisciplineView(
            store: Store(initialState: EnrollmentDisciplineFeature.State(disciplineId: 302, session: .preview)) {
                EnrollmentDisciplineFeature()
            }
        )
    }
}
