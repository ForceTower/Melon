import ComposableArchitecture
import SwiftUI

struct CourseProgressView: View {
    @Bindable var store: StoreOf<CourseProgressFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(Text(.courseProgressTitle))
        .largeNavigationBar()
        .task { await store.send(.task).finish() }
        .sheet(isPresented: explainerBinding) {
            ComplementaryHoursExplainerSheet(
                requirement: store.progress?.requirements.first { !$0.derivable }
            )
        }
        .sheet(isPresented: versionPickerBinding) {
            if let progress = store.progress {
                CurriculumVersionPickerSheet(
                    progress: progress,
                    course: store.course,
                    switchingVersionId: store.switchingVersionId,
                    onPick: { store.send(.versionSelected($0)) },
                    onAutomatic: { store.send(.automaticVersionTapped) }
                )
                .interactiveDismissDisabled(store.isSwitchingVersion)
            }
        }
        .alert($store.scope(state: \.alert, action: \.alert))
    }

    private var explainerBinding: Binding<Bool> {
        Binding(
            get: { store.isComplementaryExplainerPresented },
            set: { if !$0 { store.send(.complementaryExplainerDismissed) } }
        )
    }

    private var versionPickerBinding: Binding<Bool> {
        Binding(
            get: { store.isVersionPickerPresented },
            set: { if !$0 { store.send(.versionPickerDismissed) } }
        )
    }

    @ViewBuilder
    private var content: some View {
        if let progress = store.progress {
            loaded(progress)
        } else if store.loadFailed {
            CourseProgressFailureView { store.send(.retryTapped) }
        } else {
            CourseProgressLoadingView()
        }
    }

    // MARK: Loaded

    private func loaded(_ progress: CourseProgress) -> some View {
        ScrollView {
            VStack(spacing: 14) {
                subtitle(progress)
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 2)

                if progress.canPickVersion {
                    CourseProgressVersionButton(progress: progress, course: store.course) {
                        store.send(.versionPickerTapped)
                    }
                    .fadeUp(delay: 0.05)
                }

                if let curriculum = progress.curriculum, curriculum.stale {
                    staleNotice(curriculum)
                        .fadeUp(delay: 0.04)
                }

                CourseProgressOverallCard(progress: progress, course: store.course)
                    .scaleIn(delay: 0.08, duration: 0.62)

                if !progress.hasCurriculum {
                    UNESBanner(tone: .neutral, title: .localized(.courseProgressNoCurriculumTitle)) {
                        Text(.courseProgressNoCurriculumBody)
                    }
                    .fadeUp(delay: 0.12)
                }

                VStack(spacing: 0) {
                    CourseProgressSectionHeader(
                        title: .courseProgressSectionCurriculum,
                        hint: .localized(progress.hasCurriculum
                            ? .courseProgressSectionCurriculumHint
                            : .courseProgressSectionCurriculumHintUnavailable)
                    )
                    CourseProgressFlowchartCard(progress: progress) {
                        store.send(.flowchartTapped)
                    }
                }
                .fadeUp(delay: 0.14)

                if progress.hasCurriculum {
                    if progress.hasBreakdown {
                        breakdown(progress)
                            .fadeUp(delay: 0.2)
                        CourseProgressRemainingCard(progress: progress)
                            .fadeUp(delay: 0.24)
                    } else {
                        UNESBanner(
                            tone: .neutral,
                            title: .localized(.courseProgressNoBreakdownTitle),
                            action: .localized(.commonTryAgain),
                            onAction: { store.send(.refreshPulled) }
                        ) {
                            Text(.courseProgressNoBreakdownBody)
                        }
                        .fadeUp(delay: 0.18)
                    }
                }

                footer(progress)
                    .fadeUp(delay: 0.28)
            }
            .padding(EdgeInsets(top: 4, leading: 16, bottom: 28, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .refreshable { await store.send(.refreshPulled).finish() }
    }

    private func subtitle(_ progress: CourseProgress) -> some View {
        Text(subtitleText(progress))
            .font(.system(size: 13.5, weight: .medium))
            .tracking(-0.13)
            .foregroundStyle(UNESColor.ink3)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 4)
    }

    private func subtitleText(_ progress: CourseProgress) -> String {
        var parts: [String] = []
        if let course = store.course, !course.isEmpty {
            parts.append(course)
        }
        if let curriculum = progress.curriculum {
            parts.append(.localized(.courseProgressCurriculumCode(curriculum.codeLabel)))
        }
        if let period = progress.currentPeriod {
            parts.append(CourseProgressFormat.semester(period))
        }
        return parts.isEmpty ? .localized(.courseProgressSubtitleFallback) : parts.joined(separator: " · ")
    }

    private func staleNotice(_ curriculum: CurriculumVersion) -> some View {
        UNESBanner(
            tone: .warn,
            title: .localized(.courseProgressStaleTitle(curriculum.codeLabel))
        ) {
            if let asOf = curriculum.asOfDate {
                Text(.courseProgressStaleBody(CourseProgressFormat.asOf(asOf)))
            } else {
                Text(.courseProgressStaleBodyUndated)
            }
        }
    }

    private func breakdown(_ progress: CourseProgress) -> some View {
        VStack(spacing: 0) {
            CourseProgressSectionHeader(
                title: .courseProgressSectionRequirements,
                hint: .localized(.courseProgressSectionRequirementsHint(progress.requirements.count))
            )
            VStack(spacing: 0) {
                ForEach(Array(progress.requirements.enumerated()), id: \.element.id) { index, requirement in
                    CurriculumRequirementRow(requirement: requirement)
                    if index < progress.requirements.count - 1 {
                        CardRowDivider()
                    }
                }
            }
            .courseProgressCard()

            if progress.requirements.contains(where: { !$0.derivable }) {
                Button {
                    store.send(.complementaryExplainerTapped)
                } label: {
                    Label(String.localized(.courseProgressExplainerButton), systemImage: "doc.text")
                        .labelStyle(CompactLabelStyle())
                        .font(.system(size: 12.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .overlay {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .strokeBorder(UNESColor.cardLine)
                        }
                }
                .buttonStyle(.plain)
                .padding(.top, 8)
            }
        }
    }

    private func footer(_ progress: CourseProgress) -> some View {
        VStack(spacing: 2) {
            Text(.courseProgressFooterSynced(CourseProgressFormat.syncedAt(progress.syncedAt)))
            Text(.courseProgressFooterOfficial)
        }
        .font(.system(size: 11, weight: .medium))
        .foregroundStyle(UNESColor.ink4)
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 6, leading: 12, bottom: 4, trailing: 12))
    }

    /// Faint rose mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .rose, intensity: 0.5)
            .frame(height: 300)
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
            .opacity(0.3)
            .offset(y: -90)
            .ignoresSafeArea()
    }
}

// MARK: - Overall meter card

struct CourseProgressOverallCard: View {
    var progress: CourseProgress
    var course: String?

    private var summary: CurriculumSummary { progress.summary }
    private var unknown: Bool { !progress.hasCurriculum }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .bottom, spacing: 12) {
                VStack(alignment: .leading, spacing: 7) {
                    Text(unknown ? .courseProgressOverallHoursCompleted : .courseProgressOverallCompleted)
                        .textCase(.uppercase)
                        .font(.system(size: 11.5, weight: .semibold))
                        .tracking(0.6)
                        .foregroundStyle(UNESColor.ink4)
                    headline
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 3) {
                    Text(verbatim: unknown
                        ? "—"
                        : "\(CourseProgressFormat.count(summary.completedHours)) / \(CourseProgressFormat.hours(summary.requiredHours ?? 0))")
                        .font(.system(size: 14, weight: .semibold))
                        .monospacedDigit()
                        .tracking(-0.2)
                        .foregroundStyle(UNESColor.ink)
                    Group {
                        if unknown {
                            Text(.courseProgressTotalUnknown)
                        } else {
                            Text(.courseProgressRemainingShort(CourseProgressFormat.hours(summary.remainingHours ?? 0)))
                        }
                    }
                    .font(.system(size: 11.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(.bottom, 14)

            CurriculumMeter(
                value: summary.completedHours,
                total: summary.requiredHours,
                height: 10,
                unknown: unknown
            )

            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(contextLine)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let curriculum = progress.curriculum {
                    Text(curriculum.stale
                        ? .courseProgressCurriculumCodeStale(curriculum.codeLabel)
                        : .courseProgressCurriculumCode(curriculum.codeLabel))
                        .font(.system(size: 11.5, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(.top, 11)
        }
        .padding(EdgeInsets(top: 17, leading: 17, bottom: 16, trailing: 17))
        .courseProgressCard()
    }

    @ViewBuilder
    private var headline: some View {
        if unknown {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(CourseProgressFormat.count(summary.completedHours))
                    .font(.system(size: 44, weight: .bold))
                    .monospacedDigit()
                    .tracking(-1.7)
                    .foregroundStyle(UNESColor.ink)
                Text(.courseProgressHourUnit)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
            }
        } else {
            Text(CourseProgressFormat.headlinePercent(summary.percent ?? 0))
                .font(.system(size: 48, weight: .bold))
                .monospacedDigit()
                .tracking(-2.1)
                .foregroundStyle(UNESColor.ink)
        }
    }

    private var contextLine: String {
        var parts: [String] = []
        if let period = progress.currentPeriod {
            parts.append(CourseProgressFormat.semester(period))
        }
        if let course, !course.isEmpty {
            parts.append(course)
        }
        return parts.joined(separator: " · ")
    }
}

// MARK: - Fluxograma entry (with mini-map)

struct CourseProgressFlowchartCard: View {
    var progress: CourseProgress
    var onOpen: () -> Void

    private var disabled: Bool { !progress.hasCurriculum }

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    Image(systemName: "point.3.connected.trianglepath.dotted")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(UNESColor.ink2)
                        .frame(width: 34, height: 34)
                        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(.courseProgressFlowchartTitle)
                            .font(.system(size: 15.5, weight: .semibold))
                            .tracking(-0.3)
                            .foregroundStyle(UNESColor.ink)
                        Text(subtitle)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    }
                    Spacer(minLength: 0)
                    if !disabled {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                if !disabled {
                    CurriculumMiniMap(progress: progress)
                }
            }
            .padding(EdgeInsets(top: 15, leading: 16, bottom: 14, trailing: 16))
            .frame(maxWidth: .infinity, alignment: .leading)
            .courseProgressCard()
            .opacity(disabled ? 0.55 : 1)
        }
        .buttonStyle(.pressableCard)
        .disabled(disabled)
    }

    private var subtitle: String {
        guard !disabled else { return .localized(.courseProgressFlowchartUnavailable) }
        return .localized(.courseProgressFlowchartSubtitle(
            progress.scheduledPeriods.count,
            progress.summary.disciplinesTotal,
            progress.summary.disciplinesCompleted
        ))
    }
}

/// One thin bar per discipline, one column per período — the whole course
/// at a glance.
struct CurriculumMiniMap: View {
    var progress: CourseProgress

    var body: some View {
        VStack(spacing: 5) {
            // Bars top-aligned, labels on one baseline — períodos differ in
            // length and the numbers must still read as one row.
            HStack(alignment: .top, spacing: 4) {
                ForEach(progress.scheduledPeriods) { period in
                    VStack(spacing: 3) {
                        ForEach(period.entries) { entry in
                            bar(entry.status)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            HStack(spacing: 4) {
                ForEach(progress.scheduledPeriods) { period in
                    Text(String(period.period ?? 0))
                        .font(.system(size: 8.5, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(period.period == progress.currentPeriod ? UNESColor.accent : UNESColor.ink4)
                        .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func bar(_ status: CurriculumEntryStatus) -> some View {
        let shape = RoundedRectangle(cornerRadius: 2.5, style: .continuous)
        return shape
            .fill(fill(status))
            .frame(height: 7)
            .overlay {
                switch status {
                case .inProgress:
                    shape.strokeBorder(status.tone, lineWidth: 1.5)
                case .withdrawn:
                    shape.strokeBorder(status.tone.opacity(0.8), style: StrokeStyle(lineWidth: 1, dash: [2, 2]))
                default:
                    EmptyView()
                }
            }
    }

    private func fill(_ status: CurriculumEntryStatus) -> Color {
        switch status {
        case .completed: status.tone
        case .inProgress, .failed: status.tone.opacity(0.22)
        case .available, .withdrawn, .blocked, .notTaken: UNESColor.surface3
        }
    }
}

// MARK: - "O que falta"

struct CourseProgressRemainingCard: View {
    var progress: CourseProgress

    private var remaining: [CurriculumRequirementProgress] {
        progress.requirements
            .filter { $0.hoursRemaining > 0 }
            .sorted { $0.hoursRemaining > $1.hoursRemaining }
    }

    private var total: Int {
        remaining.reduce(0) { $0 + $1.hoursRemaining }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text(.courseProgressRemainingTitle)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.2)
                    .foregroundStyle(UNESColor.ink)
                Spacer()
                Text(CourseProgressFormat.hours(total))
                    .font(.system(size: 17, weight: .bold))
                    .monospacedDigit()
                    .tracking(-0.4)
                    .foregroundStyle(UNESColor.ink)
            }
            .padding(EdgeInsets(top: 14, leading: 15, bottom: 12, trailing: 15))
            CardRowDivider()

            VStack(spacing: 0) {
                ForEach(remaining) { requirement in
                    HStack(spacing: 10) {
                        Circle()
                            .fill(requirement.derivable ? UNESColor.ink4.opacity(0.5) : UNESColor.tangerine)
                            .frame(width: 5, height: 5)
                        Text(requirement.shortLabel)
                            .font(.system(size: 13, weight: .medium))
                            .tracking(-0.13)
                            .foregroundStyle(UNESColor.ink2)
                            .lineLimit(1)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text(CourseProgressFormat.hours(requirement.hoursRemaining))
                            .font(.system(size: 13, weight: .semibold))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink)
                    }
                    .padding(EdgeInsets(top: 8, leading: 15, bottom: 8, trailing: 15))
                }
            }
            .padding(.vertical, 4)

            CardRowDivider()
            Text(.courseProgressRemainingFootnote(progress.curriculum?.codeLabel ?? ""))
                .font(.system(size: 11.5, weight: .medium))
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink4)
                .padding(EdgeInsets(top: 10, leading: 15, bottom: 13, trailing: 15))
        }
        .courseProgressCard()
    }
}

// MARK: - Complementary hours explainer

/// Why atividades complementares sit at 0 h: the portal only records them
/// after the colegiado validates paper certificates.
struct ComplementaryHoursExplainerSheet: View {
    var requirement: CurriculumRequirementProgress?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "doc.text")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(UNESColor.tangerine)
                        .frame(width: 38, height: 38)
                        .background(UNESColor.tangerine.opacity(0.12), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(UNESColor.tangerine.opacity(0.3))
                        }
                    VStack(alignment: .leading, spacing: 3) {
                        Text(requirement?.label ?? .localized(.courseProgressExplainerTitle))
                            .font(.system(size: 20, weight: .bold))
                            .tracking(-0.6)
                            .foregroundStyle(UNESColor.ink)
                        if let requirement {
                            Text(.courseProgressExplainerCounted(
                                CourseProgressFormat.count(requirement.hoursCompleted),
                                CourseProgressFormat.count(requirement.hoursRequired)
                            ))
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                        }
                    }
                }
                .padding(.bottom, 4)

                Text(.courseProgressExplainerBody1)
                Text(.courseProgressExplainerBody2)
            }
            .font(.system(size: 14, weight: .medium))
            .lineSpacing(3)
            .foregroundStyle(UNESColor.ink2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 26, leading: 20, bottom: 28, trailing: 20))
        }
        .background(UNESColor.surface)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

// MARK: - Previews

#Preview("Progresso") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia")) {
                CourseProgressFeature()
            }
        )
    }
}

#Preview("Grade defasada") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia", progress: .preview(stale: true))) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = {}
            }
        )
    }
}

#Preview("Currículo escolhido à mão") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia", progress: .preview(manualPick: true))) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = {}
            }
        )
    }
}

#Preview("Sem detalhamento") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia", progress: .preview(breakdown: false))) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = {}
            }
        )
    }
}

#Preview("Sem currículo") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia", progress: .preview(curriculum: false))) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = {}
            }
        )
    }
}

#Preview("Carregando") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia")) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = { try await Task.sleep(for: .seconds(86_400)) }
            }
        )
    }
}

#Preview("Falha") {
    NavigationStack {
        CourseProgressView(
            store: Store(initialState: CourseProgressFeature.State(course: "Psicologia")) {
                CourseProgressFeature()
            } withDependencies: {
                $0.courseProgressRepository.observe = { .finished }
                $0.courseProgressRepository.refresh = { throw APIError.emptyEnvelope }
            }
        )
    }
}
