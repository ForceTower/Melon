import SwiftUI

// MARK: - Standing vocabulary

extension CurriculumStanding {
    /// Lower-case, for the "course · standing" subtitle; upper-cased for the row tag.
    var shortLabel: LocalizedStringResource {
        switch self {
        case .current: .courseProgressVersionStandingShortCurrent
        case .previous: .courseProgressVersionStandingShortPrevious
        case .retired: .courseProgressVersionStandingShortRetired
        case .unplaced: .courseProgressVersionStandingShortUnplaced
        }
    }

    /// The grid taking entrants today is "live"; everything else is history.
    var tone: Color {
        self == .current ? UNESBannerTone.ok : UNESColor.ink4
    }
}

// MARK: - Entry row on the progress screen

/// Which curriculum the numbers below are computed on, and the door into the
/// picker. Only shown when there is something to pick between.
struct CourseProgressVersionButton: View {
    var progress: CourseProgress
    var course: String?
    var onOpen: () -> Void

    private var standing: CurriculumStanding? {
        progress.curriculum.map(progress.standing(of:))
    }

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 11) {
                Circle()
                    .fill(standing?.tone ?? UNESColor.ink4)
                    .frame(width: 7, height: 7)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 14, weight: .semibold))
                        .monospacedDigit()
                        .tracking(-0.2)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(.courseProgressVersionCount(progress.availableVersions.count))
                    .font(.system(size: 11.5, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                Image(systemName: "chevron.down")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 10, leading: 13, bottom: 10, trailing: 13))
            .courseProgressCard(cornerRadius: 16)
        }
        .buttonStyle(.pressableCard)
    }

    private var title: String {
        guard let curriculum = progress.curriculum else { return .localized(.courseProgressVersionPick) }
        return .localized(.courseProgressVersionTitle(curriculum.codeLabel))
    }

    private var subtitle: String {
        guard let standing else { return .localized(.courseProgressVersionNone) }
        var parts: [String] = []
        if let course, !course.isEmpty {
            parts.append(course)
        }
        parts.append(.localized(standing.shortLabel))
        return parts.joined(separator: " · ")
    }
}

// MARK: - Picker sheet

/// Every version of the course, each scored against the student's own
/// history, so "which grid is mine?" is answerable from the numbers rather
/// than from memory. Picking one re-binds server-side and the screen behind
/// re-renders from the rebuilt payload.
struct CurriculumVersionPickerSheet: View {
    var progress: CourseProgress
    var course: String?
    /// The version being switched to (or `CourseProgressFeature.automaticVersionSwitch`).
    var switchingVersionId: String?
    var onPick: (String) -> Void
    var onAutomatic: () -> Void

    private var isSwitching: Bool { switchingVersionId != nil }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header
                    .padding(.bottom, 12)

                VStack(spacing: 9) {
                    ForEach(progress.availableVersions) { version in
                        CurriculumVersionRow(
                            version: version,
                            standing: progress.standing(of: version),
                            approvedHours: progress.approvedHours,
                            selected: version.id == progress.curriculum?.id,
                            switching: version.id == switchingVersionId
                        ) {
                            onPick(version.id)
                        }
                        .disabled(isSwitching)
                    }
                }

                Text(.courseProgressVersionFootnote)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2.5)
                    .foregroundStyle(UNESColor.ink4)
                    .padding(EdgeInsets(top: 11, leading: 2, bottom: 0, trailing: 2))

                if progress.curriculum?.isManualPick == true {
                    Button(action: onAutomatic) {
                        HStack(spacing: 7) {
                            if switchingVersionId == CourseProgressFeature.automaticVersionSwitch {
                                ProgressView().controlSize(.small)
                            } else {
                                Image(systemName: "wand.and.sparkles")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            Text(.courseProgressVersionAutomatic)
                        }
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
                    .disabled(isSwitching)
                    .padding(.top, 12)
                }
            }
            .padding(EdgeInsets(top: 26, leading: 20, bottom: 28, trailing: 20))
        }
        .scrollIndicators(.hidden)
        .background(UNESColor.surface)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(.courseProgressVersionSheetTitle)
                .font(.system(size: 20, weight: .bold))
                .tracking(-0.6)
                .foregroundStyle(UNESColor.ink)
            Group {
                if let course, !course.isEmpty {
                    Text(.courseProgressVersionSheetSubtitle(course))
                } else {
                    Text(.courseProgressVersionSheetSubtitleFallback)
                }
            }
            .font(.system(size: 12.5, weight: .medium))
            .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// One version: code, its place in the succession, the student's numbers
/// against it, and how much of their history it accounts for.
struct CurriculumVersionRow: View {
    var version: CurriculumVersion
    var standing: CurriculumStanding
    /// The student's whole pool of passed hours — what `fit` is a share of.
    var approvedHours: Int
    var selected: Bool
    var switching: Bool
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(.courseProgressVersionTitle(version.codeLabel))
                        .font(.system(size: 15.5, weight: .bold))
                        .monospacedDigit()
                        .tracking(-0.4)
                        .foregroundStyle(UNESColor.ink)
                    standingTag
                    Spacer(minLength: 8)
                    if switching {
                        ProgressView().controlSize(.small)
                    } else if selected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(UNESColor.accent)
                    }
                }

                Text(hint)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .multilineTextAlignment(.leading)
                    .padding(.top, 3)

                HStack(alignment: .firstTextBaseline) {
                    Text(hoursLine)
                        .font(.system(size: 12, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                    Spacer(minLength: 8)
                    if let percent = version.percent, version.requiredHours != nil {
                        Text(CourseProgressFormat.headlinePercent(percent))
                            .font(.system(size: 13, weight: .bold))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink)
                    }
                }
                .padding(.top, 13)

                CurriculumMeter(
                    value: version.completedHours ?? 0,
                    total: version.requiredHours,
                    tone: selected ? UNESBannerTone.ok : UNESBannerTone.ok.opacity(0.45),
                    height: 6,
                    unknown: version.requiredHours == nil
                )
                .padding(.top, 7)

                Label(fitLine, systemImage: "arrow.triangle.2.circlepath")
                    .labelStyle(CompactLabelStyle())
                    .font(.system(size: 11, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 8)
            }
            .padding(EdgeInsets(top: 14, leading: 15, bottom: 13, trailing: 15))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(selected ? UNESColor.accent : UNESColor.cardLine, lineWidth: selected ? 1.5 : 1)
            }
        }
        .buttonStyle(.pressableCard)
    }

    private var standingTag: some View {
        Text(standing.shortLabel)
            .font(.system(size: 10.5, weight: .bold))
            .tracking(0.2)
            .textCase(.uppercase)
            .foregroundStyle(standing.tone)
            .padding(EdgeInsets(top: 2, leading: 7, bottom: 2, trailing: 7))
            .background(standing.tone.opacity(0.11), in: Capsule())
            .lineLimit(1)
            .fixedSize()
    }

    private var hint: String {
        switch standing {
        case .current:
            return .localized(.courseProgressVersionHintCurrent)
        case .previous, .retired:
            guard let successor = version.supersededBy else { return .localized(.courseProgressVersionHintUnplaced) }
            if let effective = successor.effectiveFromLabel {
                return .localized(.courseProgressVersionHintSuperseded(successor.codeLabel, effective))
            }
            return .localized(.courseProgressVersionHintSupersededUndated(successor.codeLabel))
        case .unplaced:
            return .localized(.courseProgressVersionHintUnplaced)
        }
    }

    private var hoursLine: String {
        let completed = version.completedHours ?? 0
        guard let required = version.requiredHours else {
            return .localized(.courseProgressVersionHoursOnly(CourseProgressFormat.hours(completed)))
        }
        return "\(CourseProgressFormat.count(completed))/\(CourseProgressFormat.hours(required))"
    }

    private var fitLine: String {
        guard let fit = version.fit else { return .localized(.courseProgressVersionFitUnknown) }
        return .localized(.courseProgressVersionFit(
            CourseProgressFormat.percent(fit, fractionDigits: 0),
            CourseProgressFormat.hours(approvedHours)
        ))
    }
}

// MARK: - Previews

#Preview("Seletor") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            CurriculumVersionPickerSheet(
                progress: .preview(),
                course: "Psicologia",
                switchingVersionId: nil,
                onPick: { _ in },
                onAutomatic: {}
            )
        }
}

#Preview("Botão") {
    VStack {
        CourseProgressVersionButton(progress: .preview(), course: "Psicologia") {}
        CourseProgressVersionButton(progress: .preview(curriculum: false), course: "Psicologia") {}
    }
    .padding(16)
    .background(UNESColor.surface)
}
