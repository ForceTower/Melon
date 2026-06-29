import SwiftUI

// UNES — the proposal laid onto the weekly grid, with a conflict summary, a
// note for sections still missing a schedule, and a color legend. Ported from
// `TimetableScreen` in `screens-matricula-screens.jsx`.
struct EnrollmentTimetableView: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let onReview: () -> Void

    private var scheduled: [TimetableItem] {
        enroll.picks
            .filter { EnrollmentScheduling.hasSchedule($0.section) }
            .map { TimetableItem(discipline: $0.discipline, section: $0.section) }
    }

    private var tbdCount: Int {
        enroll.picks.filter { !EnrollmentScheduling.hasSchedule($0.section) }.count
    }

    private var isEmpty: Bool { enroll.picks.isEmpty }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .cool, intensity: 0.5).opacity(0.18)
                    LinearGradient(
                        colors: [.clear, UNESColor.surface],
                        startPoint: .top, endPoint: .bottom
                    )
                }
                .frame(height: 200)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    conflictSummary
                        .fadeUpOnAppear(delay: 0.04, distance: 12, duration: 0.5)
                    WeeklyTimetable(items: scheduled)
                        .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)
                    if tbdCount > 0 {
                        tbdNote
                            .fadeUpOnAppear(delay: 0.16, distance: 10, duration: 0.5)
                    }
                    if !isEmpty {
                        legend
                            .fadeUpOnAppear(delay: 0.2, distance: 10, duration: 0.5)
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 6)
                .padding(.bottom, 120)
            }
        }
        .overlay(alignment: .bottom) {
            EnrollmentDock(
                enroll: enroll,
                window: window,
                primaryLabel: "Revisar",
                primaryAction: onReview
            )
        }
        .navigationTitle("Sua grade")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
    }

    // MARK: Conflict summary

    @ViewBuilder
    private var conflictSummary: some View {
        let conflicts = enroll.conflicts
        if !conflicts.isEmpty {
            EnrollmentBanner(
                tone: .danger,
                title: "\(conflicts.count) conflito\(conflicts.count > 1 ? "s" : "") de horário"
            ) {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(conflicts) { c in
                        Text("\(c.a.code) \(c.aLabel) × \(c.b.code) \(c.bLabel) · \(EnrollmentScheduling.daysFull[c.day].lowercased())")
                    }
                }
            }
        } else if isEmpty {
            EnrollmentBanner(tone: .info, title: "Grade vazia", systemImage: "circle") {
                Text("Selecione turmas no catálogo para vê-las aqui. Conflitos aparecem em vermelho automaticamente.")
            }
        } else {
            EnrollmentBanner(tone: .info, title: "Sem conflitos", systemImage: "checkmark") {
                Text("Suas \(scheduled.count) turmas com horário encaixam sem sobreposição.")
            }
        }
    }

    // MARK: TBD note

    private var tbdNote: some View {
        HStack(spacing: 8) {
            Circle().fill(UNESColor.ink4).frame(width: 6, height: 6)
            Text("\(tbdCount) turma\(tbdCount > 1 ? "s" : "") com horário a definir — não exibida\(tbdCount > 1 ? "s" : "") na grade.")
                .font(UNESFont.sans(11.5))
                .foregroundStyle(UNESColor.ink3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 4)
    }

    // MARK: Legend

    private var legend: some View {
        FlowChips(picks: enroll.picks)
    }
}

/// Wrapping row of "● CODE label" legend chips, one per pick.
private struct FlowChips: View {
    let picks: [EnrollmentPick]

    var body: some View {
        // Chips flow left→right, wrapping by width (shared module FlowLayout).
        FlowLayout(spacing: 8, rowSpacing: 8) {
            ForEach(picks) { pick in
                HStack(spacing: 7) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(pick.section.tone.color)
                        .frame(width: 9, height: 9)
                    Text(pick.discipline.code)
                        .font(UNESFont.mono(10, weight: .semibold))
                        .foregroundStyle(UNESColor.ink2)
                    Text(pick.section.label)
                        .font(UNESFont.mono(9))
                        .foregroundStyle(UNESColor.ink4)
                }
                .padding(.horizontal, 11)
                .padding(.vertical, 6)
                .cardSurface(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
        }
    }
}

#Preview {
    NavigationStack {
        EnrollmentTimetableView(
            enroll: .previewSeeded,
            window: EnrollmentFixtures.window,
            onReview: {}
        )
    }
}
