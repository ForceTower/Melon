import SwiftUI

/// "Presença" block — the full absence-tracking card, with a count of
/// used/allowed, the remaining slot count, and a warning banner once the
/// ratio crosses 50%.
struct DisciplineFaltasBlock: View {
    let discipline: Discipline

    private var ratio: Double {
        Double(discipline.absences) / Double(max(1, discipline.allowedAbsences))
    }

    private var tone: Color {
        if ratio >= 0.75 { return DisciplineScoreColor.danger }
        if ratio >= 0.50 { return DisciplineScoreColor.caution }
        return UNESColor.ink3
    }

    private var remaining: Int {
        max(0, discipline.allowedAbsences - discipline.absences)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            DisciplineSectionHeader("Presença")

            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .bottom) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .lastTextBaseline, spacing: 0) {
                            Text("\(discipline.absences)")
                                .font(UNESFont.serif(28))
                                .tracking(-0.56)
                                .foregroundStyle(tone)
                            Text(" de \(discipline.allowedAbsences)")
                                .font(UNESFont.serif(18))
                                .foregroundStyle(UNESColor.ink4)
                        }
                        Text("faltas até agora")
                            .font(UNESFont.sans(12))
                            .foregroundStyle(UNESColor.ink3)
                    }

                    Spacer(minLength: 12)

                    VStack(alignment: .trailing, spacing: 4) {
                        Text("\(remaining)")
                            .font(UNESFont.serif(20))
                            .tracking(-0.4)
                            .foregroundStyle(UNESColor.ink)
                        Text("AINDA PODEM")
                            .font(UNESFont.mono(9, weight: .semibold))
                            .tracking(0.9)
                            .foregroundStyle(UNESColor.ink4)
                    }
                }

                AbsenceBar(used: discipline.absences, allowed: discipline.allowedAbsences)

                if ratio >= 0.5 {
                    HStack(spacing: 8) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 11))
                        Text(ratio >= 0.75
                             ? "Atenção: próximo do limite de faltas."
                             : "Monitore suas faltas com cuidado.")
                            .font(UNESFont.sans(11))
                    }
                    .foregroundStyle(tone)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(tone.opacity(0.13))
                    )
                }
            }
            .padding(16)
            .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 18)
    }
}
