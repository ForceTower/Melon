import SwiftUI

/// The discipline card: status, what it needs and what it unlocks — the one
/// place the prerequisite direction is spelled out. Prerequisite rows push
/// their own card inside the sheet, so a chain like
/// CHF299 → CHF344 → CHF345 reads with native back navigation.
struct CurriculumEntrySheet: View {
    var progress: CourseProgress
    var entry: CurriculumEntry
    var onTrail: (String) -> Void

    @State private var path: [String] = []
    /// Measured content height so the sheet hugs whichever card is showing —
    /// same pattern as `MeDocumentSheet`, including the post-presentation
    /// re-sync (a detent write issued mid-animation is dropped).
    @State private var height: CGFloat = 420

    var body: some View {
        NavigationStack(path: $path) {
            CurriculumEntryDetail(progress: progress, entry: entry, isRoot: true, onTrail: onTrail, onHeight: fit)
                .navigationDestination(for: String.self) { code in
                    if let pushed = progress.entry(code) {
                        CurriculumEntryDetail(progress: progress, entry: pushed, isRoot: false, onTrail: onTrail, onHeight: fit)
                    }
                }
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
    }

    private func fit(_ measured: CGFloat) {
        height = measured
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(700))
            if height == measured {
                height = measured + 0.001
            }
        }
    }
}

private struct CurriculumEntryDetail: View {
    var progress: CourseProgress
    var entry: CurriculumEntry
    var isRoot: Bool
    var onTrail: (String) -> Void
    var onHeight: (CGFloat) -> Void

    /// Room for the system bar pushed cards carry above their content.
    private static let pushedBarAllowance: CGFloat = 56

    @Environment(\.dismiss) private var dismiss

    private var unlocks: [CurriculumEntry] {
        progress.unlocks(of: entry.code)
    }

    private var prerequisites: [CurriculumEntry] {
        entry.prerequisites.compactMap(progress.entry)
    }

    private var corequisites: [CurriculumEntry] {
        progress.corequisites(of: entry.code)
    }

    /// Prerequisites the student still owes — the ones actually holding
    /// this discipline back.
    private var missing: [CurriculumEntry] {
        prerequisites.filter { $0.status != .completed }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 13) {
                heading
                CurriculumStatusChip(status: entry.status, full: true)
                statusNotice
                facts
                relations(
                    title: .courseProgressDependsOn,
                    entries: prerequisites,
                    codes: entry.prerequisites,
                    empty: .courseProgressNoPrerequisites
                )
                if !corequisites.isEmpty {
                    relations(
                        title: .courseProgressTakenAlongside,
                        entries: corequisites,
                        codes: corequisites.map(\.code),
                        empty: .courseProgressNoPrerequisites,
                        trailingIcon: "arrow.left.arrow.right"
                    )
                }
                relations(
                    title: .courseProgressUnlocks,
                    entries: unlocks,
                    codes: unlocks.map(\.code),
                    empty: .courseProgressUnlocksNothing,
                    trailingIcon: "arrow.right"
                )
                if !entry.prerequisites.isEmpty || !unlocks.isEmpty {
                    Button {
                        onTrail(entry.code)
                    } label: {
                        Label(String.localized(.courseProgressShowTrail), systemImage: "point.3.connected.trianglepath.dotted")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .buttonStyle(.unesDark)
                    .padding(.top, 2)
                }
            }
            .padding(EdgeInsets(top: isRoot ? 22 : 8, leading: 18, bottom: 26, trailing: 18))
            .onGeometryChange(for: CGFloat.self) { proxy in
                proxy.size.height
            } action: { measured in
                onHeight(measured + (isRoot ? 0 : Self.pushedBarAllowance))
            }
        }
        .scrollIndicators(.hidden)
        .background(UNESColor.surface)
        .navigationTitle(entry.code)
        .inlineNavigationBar()
        // The root card carries its own header row (title + close), like the
        // other sheets; only pushed prerequisite cards get the system bar.
        .navigationBarHiddenCompat(isRoot)
    }

    private var heading: some View {
        HStack(alignment: .top, spacing: 12) {
            CurriculumStatusBadge(status: entry.status, size: 38, cornerRadius: 12)
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.code)
                    .font(.system(size: 11.5, weight: .bold))
                    .tracking(0.6)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                Text(entry.name)
                    .font(.system(size: 20, weight: .bold))
                    .tracking(-0.6)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if isRoot {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(width: 30, height: 30)
                        .background(UNESColor.surface2, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text(.commonDone))
            }
        }
    }

    @ViewBuilder
    private var statusNotice: some View {
        switch entry.status {
        case .withdrawn:
            UNESBanner(tone: .warn, title: .localized(.courseProgressWithdrawnTitle)) {
                Text(.courseProgressWithdrawnBody)
            }
        case .failed:
            UNESBanner(tone: .danger, title: .localized(.courseProgressFailedTitle)) {
                Text(.courseProgressFailedBody)
            }
        case .blocked where !missing.isEmpty:
            UNESBanner(tone: .neutral, title: .localized(.courseProgressBlockedTitle(missing.count))) {
                Text(missing.map { "\($0.code) \($0.name) (\(String.localized($0.status.label).lowercased()))" }
                    .joined(separator: " · "))
            }
        case .available:
            UNESBanner(tone: .info, title: .localized(.courseProgressAvailableTitle)) {
                Text(entry.prerequisites.isEmpty ? .courseProgressAvailableBodyNoPrereqs : .courseProgressAvailableBody)
            }
        case .completed, .inProgress, .blocked, .notTaken:
            EmptyView()
        }
    }

    private var facts: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
            fact(.courseProgressFactPeriod, value: entry.period.map { CourseProgressFormat.ordinal($0) } ?? .localized(.courseProgressFactElective))
            fact(.courseProgressFactHours, value: CourseProgressFormat.hours(entry.hours))
            if let requirement = progress.requirementLabel(for: entry) {
                fact(.courseProgressFactRequirement, value: requirement)
            }
            if let curriculum = progress.curriculum {
                fact(.courseProgressFactCurriculum, value: curriculum.codeLabel)
            }
        }
        .padding(.vertical, 3)
    }

    private func fact(_ label: LocalizedStringResource, value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .font(.system(size: 10.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
            Text(value)
                .font(.system(size: 13.5, weight: .semibold))
                .tracking(-0.2)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 10, leading: 12, bottom: 10, trailing: 12))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    /// "Depende de" / "Libera": each item carries its own status and pushes
    /// its card. Codes with no entry in this curriculum (an equivalence, or
    /// one not transcribed) still show as plain codes so nothing is hidden.
    private func relations(
        title: LocalizedStringResource,
        entries: [CurriculumEntry],
        codes: [String],
        empty: LocalizedStringResource,
        trailingIcon: String? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Text(title)
                if let trailingIcon {
                    Image(systemName: trailingIcon)
                        .font(.system(size: 10, weight: .bold))
                }
            }
            .textCase(.uppercase)
            .font(.system(size: 11.5, weight: .semibold))
            .tracking(0.5)
            .foregroundStyle(UNESColor.ink4)

            if codes.isEmpty {
                Text(empty)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.horizontal, 2)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(codes.enumerated()), id: \.element) { index, code in
                        if let related = entries.first(where: { $0.code == code }) {
                            NavigationLink(value: code) {
                                CurriculumRelatedEntryRow(entry: related)
                            }
                            .buttonStyle(.plain)
                        } else {
                            Text(code)
                                .font(.system(size: 13, weight: .semibold))
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink3)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(EdgeInsets(top: 12, leading: 13, bottom: 12, trailing: 13))
                        }
                        if index < codes.count - 1 {
                            CardRowDivider()
                        }
                    }
                }
                .courseProgressCard()
            }
        }
    }
}

#Preview("Ficha") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            CurriculumEntrySheet(
                progress: .preview(),
                entry: CourseProgress.preview().entry("CHF345")!,
                onTrail: { _ in }
            )
        }
}
