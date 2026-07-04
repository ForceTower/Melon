#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// The watch discipline drill-down: partial average hero, grades, absences.
struct WatchDisciplineView: View {
    let store: StoreOf<WatchAppFeature>
    var disciplineId: String

    var body: some View {
        if let discipline = store.snapshot?.disciplines.first(where: { $0.id == disciplineId }) {
            content(discipline)
                .navigationTitle(discipline.code)
        } else {
            Text(.homeDayEmpty)
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private func content(_ discipline: WatchSnapshot.Discipline) -> some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let color = UNESColor.disciplineReadableColor(discipline.colorIndex)
            List {
                Section {
                    header(discipline)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 4, trailing: 4))
                    hero(discipline)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets())
                }
                gradesSection(discipline, color: color, now: now)
                absencesSection(discipline)
            }
        }
    }

    private func header(_ discipline: WatchSnapshot.Discipline) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(discipline.name)
                .font(.system(size: 17, weight: .bold))
                .tracking(-0.3)
                .foregroundStyle(UNESColor.ink)
            if let teacher = discipline.teacherName {
                Text(teacher)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
        }
    }

    private func hero(_ discipline: WatchSnapshot.Discipline) -> some View {
        WatchMeshCard(
            variant: Self.meshVariant(discipline.colorIndex),
            wash: UNESColor.disciplineColor(discipline.colorIndex)
        ) {
            HStack(spacing: 10) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(.watchPartialTitle)
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.4)
                        .textCase(.uppercase)
                        .foregroundStyle(.white.opacity(0.78))
                    Text(formatGrade(discipline.partialAverage))
                        .font(.system(size: 34, weight: .bold))
                        .tracking(-1)
                        .monospacedDigit()
                        .foregroundStyle(.white)
                    Text(.watchGradesReleased(discipline.releasedCount, discipline.grades.count))
                        .font(.system(size: 11, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.7))
                }
                Spacer(minLength: 4)
                WatchRing(
                    fraction: (discipline.partialAverage ?? 0) / 10,
                    size: 52,
                    stroke: 5,
                    track: .white.opacity(0.22)
                ) {
                    Text(verbatim: "/10")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
        }
    }

    private func gradesSection(_ discipline: WatchSnapshot.Discipline, color: Color, now: Date) -> some View {
        Section {
            if discipline.grades.isEmpty {
                Text(.disciplinesDetailNoGrades)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            } else {
                ForEach(discipline.grades) { grade in
                    gradeRow(grade, color: color, now: now)
                }
            }
        } header: {
            sectionHeader(.disciplinesDetailGrades)
        }
    }

    private func gradeRow(_ grade: WatchSnapshot.Grade, color: Color, now: Date) -> some View {
        let released = grade.value != nil
        return HStack(spacing: 8) {
            WatchCodeChip(text: grade.label, color: released ? color : UNESColor.ink4)
            VStack(alignment: .leading, spacing: 1) {
                Text(grade.name)
                    .font(.system(size: 14, weight: .semibold))
                    .tracking(-0.2)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                if let date = WatchFormat.gradeDate(stamp: grade.date, now: now) {
                    Text(date)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(released ? UNESColor.ink4 : color)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 4)
            Text(formatGrade(grade.value))
                .font(.system(size: 18, weight: .bold))
                .tracking(-0.4)
                .monospacedDigit()
                .foregroundStyle(released ? UNESColor.score(grade.value) : UNESColor.ink4)
        }
    }

    private func absencesSection(_ discipline: WatchSnapshot.Discipline) -> some View {
        let allowed = discipline.allowedMissedHours
        let missed = discipline.missedHours
        let tone: Color = switch discipline.absenceRisk {
        case .ok: UNESColor.ink
        case .warning: UNESColor.caution
        case .critical: UNESColor.alertRed
        }
        let segments = min(allowed, 15)
        let filled = allowed > 0
            ? Int((Double(missed) / Double(allowed) * Double(segments)).rounded())
            : segments
        return Section {
            VStack(alignment: .leading, spacing: 8) {
                HStack(alignment: .firstTextBaseline, spacing: 5) {
                    Text("\(missed)")
                        .font(.system(size: 24, weight: .bold))
                        .tracking(-0.5)
                        .monospacedDigit()
                        .foregroundStyle(tone)
                    Text(.watchAbsencesOf(allowed))
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                    Spacer()
                    Text(.watchAbsencesRemaining(max(0, allowed - missed)))
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                }
                HStack(spacing: 2.5) {
                    ForEach(0..<segments, id: \.self) { index in
                        Capsule()
                            .fill(index < filled ? tone : Color.white.opacity(0.12))
                            .frame(height: 7)
                    }
                }
            }
            .padding(.vertical, 2)
        } header: {
            sectionHeader(.disciplinesAbsences)
        }
    }

    private func sectionHeader(_ resource: LocalizedStringResource) -> some View {
        Text(resource)
            .font(.system(size: 12, weight: .bold))
            .tracking(0.4)
            .textCase(.uppercase)
            .foregroundStyle(UNESColor.ink3)
    }

    /// Every discipline gets a mesh family, cycled the same way its color is.
    private static func meshVariant(_ colorIndex: Int) -> MeshView.Variant {
        let variants: [MeshView.Variant] = [.warm, .cool, .sun, .rose, .fresh]
        return variants[abs(colorIndex) % variants.count]
    }
}

#Preview {
    NavigationStack {
        WatchDisciplineView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            },
            disciplineId: "d1"
        )
    }
}
#endif
