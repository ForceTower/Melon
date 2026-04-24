import SwiftUI

/// Grade calculator ("Final Countdown"). Given a discipline and a set of
/// evaluation rows, the screen tells the student their verdict — passed,
/// indo pra Final, reprovada, etc. — and what they still need to do.
///
/// This is an offline/what-if tool: state lives entirely in the view. The
/// `seed` discipline only drives the context chip at the top; the math
/// doesn't touch upstream data. `ForcedMode` overrides the row set for
/// design-review screenshots — each value maps to a canned scenario.
struct FinalCountdownView: View {
    let seed: Discipline?
    let forcedMode: ForcedMode?

    @State private var rows: [FCRow] = Self.defaultRows
    @State private var weighted: Bool = false

    init(seed: Discipline? = nil, forcedMode: ForcedMode? = nil) {
        self.seed = seed
        self.forcedMode = forcedMode
    }

    private var verdict: FCVerdict {
        FinalCountdownMath.verdict(for: rows, weighted: weighted)
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Warm mesh backdrop fading into the surface.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.2)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.4),
                            .init(color: UNESColor.surface, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 280)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    FCHeader()
                        .fadeUpOnAppear(delay: 0.02, distance: 14, duration: 0.55)

                    VStack(alignment: .leading, spacing: 12) {
                        if let seed {
                            FCDisciplineChip(discipline: seed)
                                .fadeUpOnAppear(delay: 0.08, distance: 12, duration: 0.55)
                        }

                        FCVerdictHero(verdict: verdict, weighted: weighted)
                            .fadeUpOnAppear(delay: 0.14, distance: 14, duration: 0.6)
                            .animation(.spring(response: 0.45, dampingFraction: 0.82), value: verdict.kind)

                        FCBreakdown(rows: rows, weighted: weighted)
                            .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)

                        FCWeightedToggle(weighted: $weighted)
                            .fadeUpOnAppear(delay: 0.28, distance: 10, duration: 0.5)

                        evaluationsHeader
                            .fadeUpOnAppear(delay: 0.32, distance: 10, duration: 0.5)

                        VStack(spacing: 8) {
                            ForEach($rows) { $row in
                                FCGradeRow(
                                    row: $row,
                                    weighted: weighted,
                                    canRemove: rows.count > 1,
                                    onRemove: { remove(row) }
                                )
                            }
                            addRowButton
                        }
                        .fadeUpOnAppear(delay: 0.38, distance: 10, duration: 0.5)

                        quickActions
                            .padding(.top, 2)
                            .fadeUpOnAppear(delay: 0.44, distance: 10, duration: 0.5)

                        legendFooter
                            .fadeUpOnAppear(delay: 0.5, distance: 10, duration: 0.5)

                        signature
                            .frame(maxWidth: .infinity)
                            .padding(.top, 14)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 110)
                }
            }
            .scrollDismissesKeyboard(.immediately)
        }
        // Keep the system nav bar (preserves the back chevron and the
        // interactive swipe gesture) but let the header's warm-mesh wash
        // show through behind it.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { EmptyView() }
        }
        .onAppear { applyForcedMode() }
        .onChange(of: forcedMode) { _, _ in applyForcedMode() }
    }

    // MARK: - Sections

    private var evaluationsHeader: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 4) {
                Text("◦ SUAS AVALIAÇÕES")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)
                // "Preencha o que *já rolou*" — the emphasis word flips to the
                // accent italic serif.
                (
                    Text("Preencha o que ")
                        .font(UNESFont.serif(20))
                        .foregroundColor(UNESColor.ink)
                    +
                    Text("já tirou")
                        .font(UNESFont.serif(20, italic: true))
                        .foregroundColor(UNESColor.accent)
                )
                .tracking(-0.3)
            }
            Spacer()
            HStack(spacing: 4) {
                Image(systemName: "star.fill")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255))
                Text("CURINGA")
                    .font(UNESFont.mono(9))
                    .tracking(0.72)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(.top, 4)
    }

    private var addRowButton: some View {
        Button(action: addRow) {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 11, weight: .medium))
                Text("Adicionar avaliação")
                    .font(UNESFont.sans(12.5, weight: .medium))
                    .tracking(-0.06)
            }
            .foregroundStyle(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(
                        UNESColor.line,
                        style: StrokeStyle(lineWidth: 1.5, dash: [4, 4])
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private var quickActions: some View {
        HStack(spacing: 8) {
            Button(action: { /* TODO: persist scenario */ }) {
                HStack(spacing: 6) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Salvar cenário")
                        .font(UNESFont.sans(12.5, weight: .semibold))
                        .tracking(-0.06)
                }
                .foregroundStyle(UNESColor.surface)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 11)
                .padding(.horizontal, 14)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(UNESColor.ink)
                )
            }
            .buttonStyle(.plain)

            Button(action: reset) {
                Text("Limpar")
                    .font(UNESFont.sans(12.5, weight: .medium))
                    .tracking(-0.06)
                    .foregroundStyle(UNESColor.ink2)
                    .padding(.vertical, 11)
                    .padding(.horizontal, 18)
                    .background(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .fill(UNESColor.card)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                            )
                    )
            }
            .buttonStyle(.plain)
        }
    }

    private var legendFooter: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle")
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255))
                .frame(width: 22, height: 22)
                .background(
                    RoundedRectangle(cornerRadius: 7, style: .continuous)
                        .fill(Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255).opacity(0.15))
                )

            legendText
                .font(UNESFont.sans(11.5))
                .tracking(-0.06)
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
        }
        .padding(14)
        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var legendText: Text {
        Text("Cálculo não oficial. A regra varia por departamento — a UEFS considera aprovação direta com média ")
        + Text("≥ 7,0").font(UNESFont.sans(11.5, weight: .semibold)).foregroundColor(UNESColor.ink)
        + Text(", final para ")
        + Text("3,0 ≤ m < 7,0").font(UNESFont.sans(11.5, weight: .semibold)).foregroundColor(UNESColor.ink)
        + Text(" e reprovação direta para ")
        + Text("m < 3,0").font(UNESFont.sans(11.5, weight: .semibold)).foregroundColor(UNESColor.ink)
        + Text(". Marque uma avaliação como ★ curinga para ver o que falta.")
    }

    private var signature: some View {
        VStack(spacing: 3) {
            Text("final countdown")
                .font(UNESFont.serif(15, italic: true))
                .tracking(-0.08)
                .foregroundStyle(UNESColor.ink3)
            Text("FAÇA AS CONTAS ANTES QUE A FINAL FAÇA VOCÊ")
                .font(UNESFont.mono(9))
                .tracking(1.26)
                .foregroundStyle(UNESColor.ink4)
        }
        .padding(.vertical, 8)
    }

    // MARK: - Row actions

    private func addRow() {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.78)) {
            rows.append(FCRow(label: "VA\(rows.count + 1)"))
        }
    }

    private func remove(_ row: FCRow) {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.82)) {
            rows.removeAll { $0.id == row.id }
        }
    }

    private func reset() {
        withAnimation(.spring(response: 0.4, dampingFraction: 0.82)) {
            rows = Self.defaultRows
            weighted = false
        }
    }

    private func applyForcedMode() {
        guard let forcedMode else { return }
        withAnimation(.spring(response: 0.45, dampingFraction: 0.82)) {
            rows = forcedMode.rows
        }
    }

    private static var defaultRows: [FCRow] {
        [
            FCRow(label: "VA1", score: 6.5),
            FCRow(label: "VA2", score: 5.2),
            FCRow(label: "Trab", score: nil, wildcard: true),
        ]
    }
}

// MARK: - Forced modes (design review)

extension FinalCountdownView {
    /// Canned row sets used to screenshot each verdict state. Matches
    /// `FORCED_ROWS` in `screens-final-countdown.jsx`.
    enum ForcedMode: Hashable {
        case passed, onTrack, borderline, final, impossible, failed

        var rows: [FCRow] {
            switch self {
            case .passed:
                return [
                    FCRow(label: "VA1", score: 8.5),
                    FCRow(label: "VA2", score: 7.8),
                    FCRow(label: "Trab", score: 9.0, wildcard: true),
                ]
            case .final:
                return [
                    FCRow(label: "VA1", score: 5.5),
                    FCRow(label: "VA2", score: 4.0),
                    FCRow(label: "Trab", score: 6.2, wildcard: true),
                ]
            case .failed:
                return [
                    FCRow(label: "VA1", score: 1.5),
                    FCRow(label: "VA2", score: 2.0),
                    FCRow(label: "Trab", score: 2.8, wildcard: true),
                ]
            case .impossible:
                return [
                    FCRow(label: "VA1", score: 2.0),
                    FCRow(label: "VA2", score: 3.0),
                    FCRow(label: "Trab", score: 3.5, wildcard: true),
                ]
            case .borderline:
                return [
                    FCRow(label: "VA1", score: 6.5),
                    FCRow(label: "VA2", score: 5.2),
                    FCRow(label: "Trab", score: nil, wildcard: true),
                ]
            case .onTrack:
                return [
                    FCRow(label: "VA1", score: 8.0),
                    FCRow(label: "VA2", score: 7.5),
                    FCRow(label: "Trab", score: nil, wildcard: true),
                ]
            }
        }
    }
}

// MARK: - Header + Chip + Toggle

/// Screen-level header: eyebrow + "CALCULADORA · OFFLINE" badge on the top
/// row, then the "Dá pra passar?" display title. The back chevron comes from
/// the system nav bar, so there's no custom button here.
private struct FCHeader: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text("◦ FINAL COUNTDOWN")
                    .font(UNESFont.mono(10, weight: .medium))
                    .tracking(1.2)
                    .foregroundStyle(UNESColor.ink3)

                Spacer()

                HStack(spacing: 6) {
                    Circle()
                        .fill(UNESColor.accent)
                        .frame(width: 5, height: 5)
                    Text("CALCULADORA · OFFLINE")
                        .font(UNESFont.mono(9.5))
                        .tracking(1.33)
                        .foregroundStyle(UNESColor.ink4)
                }
            }
            .padding(.bottom, 8)

            (
                Text("Dá pra")
                    .font(UNESFont.serif(38, italic: true))
                    .foregroundColor(UNESColor.accent)
                +
                Text(" passar?")
                    .font(UNESFont.serif(38))
                    .foregroundColor(UNESColor.ink)
            )
            .tracking(-0.76)

            Text("Preencha as avaliações que já rolaram e veja, na matemática fria, quanto você precisa pra passar.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
                .padding(.top, 10)
                .frame(maxWidth: 300, alignment: .leading)
        }
        .padding(.horizontal, 20)
        .padding(.top, 4)
        .padding(.bottom, 14)
    }
}

/// Context chip showing which discipline we're calculating for. The "trocar"
/// button is decorative for now — discipline swapping lands in a later pass.
private struct FCDisciplineChip: View {
    let discipline: Discipline

    var body: some View {
        HStack(spacing: 10) {
            Text(codeBadge)
                .font(UNESFont.mono(10, weight: .semibold))
                .tracking(0.2)
                .foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(discipline.color)
                )
                .shadow(color: discipline.color.opacity(0.2), radius: 8, x: 0, y: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text(discipline.title)
                    .font(UNESFont.sans(13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text("\(discipline.prof) · \(discipline.semesterId ?? "2026.1")")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.57)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Text("TROCAR")
                .font(UNESFont.mono(9))
                .tracking(0.9)
                .foregroundStyle(UNESColor.ink3)
                .padding(.horizontal, 9)
                .padding(.vertical, 5)
                .background(
                    Capsule()
                        .fill(UNESColor.surface2)
                        .overlay(Capsule().strokeBorder(UNESColor.line, lineWidth: 1))
                )
        }
        .padding(12)
        .cardSurface(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var codeBadge: String {
        // Compact 3-char code extracted from the short discipline code.
        // "CII" ← "CALC II" · "ALG" ← "ALGI II" · else first 3 letters.
        let trimmed = discipline.code.replacingOccurrences(of: " ", with: "")
        return String(trimmed.uppercased().prefix(3))
    }
}

/// Pill switch flipping between simple and weighted average. The inner
/// 34×20 knob slides horizontally; the label underneath swaps to match.
private struct FCWeightedToggle: View {
    @Binding var weighted: Bool

    var body: some View {
        Button {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.78)) {
                weighted.toggle()
            }
        } label: {
            HStack(spacing: 10) {
                knob
                VStack(alignment: .leading, spacing: 2) {
                    Text("Modo ponderado")
                        .font(UNESFont.sans(12.5, weight: .semibold))
                        .tracking(-0.06)
                    Text(weighted
                         ? "PESOS ATIVOS · ARRASTE ×"
                         : "MÉDIA SIMPLES · TODAS VALEM IGUAL")
                        .font(UNESFont.mono(9))
                        .tracking(0.72)
                        .opacity(0.65)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: "scalemass")
                    .font(.system(size: 13, weight: .medium))
            }
            .foregroundStyle(weighted ? UNESColor.surface : UNESColor.ink)
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(weighted ? UNESColor.ink : UNESColor.card)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(weighted ? UNESColor.ink : UNESColor.cardLine, lineWidth: 1)
                    )
            )
        }
        .buttonStyle(.plain)
    }

    private var knob: some View {
        ZStack(alignment: weighted ? .trailing : .leading) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(weighted ? UNESColor.surface : UNESColor.surface3)
                .frame(width: 34, height: 20)
            Circle()
                .fill(weighted ? UNESColor.ink : UNESColor.surface)
                .frame(width: 16, height: 16)
                .padding(.horizontal, 2)
                .shadow(color: .black.opacity(0.15), radius: 2, x: 0, y: 1)
        }
        .frame(width: 34, height: 20)
    }
}

#if DEBUG
    #Preview("Borderline") {
        NavigationStack {
            FinalCountdownView(
                seed: DisciplineFixtures.semesters
                    .first(where: { $0.id == DisciplineFixtures.currentSemesterId })?
                    .disciplines.first,
                forcedMode: nil
            )
        }
    }

    #Preview("Final") {
        NavigationStack {
            FinalCountdownView(
                seed: DisciplineFixtures.semesters
                    .first(where: { $0.id == DisciplineFixtures.currentSemesterId })?
                    .disciplines.first,
                forcedMode: .final
            )
        }
    }

    #Preview("Passed") {
        NavigationStack {
            FinalCountdownView(
                seed: DisciplineFixtures.semesters
                    .first(where: { $0.id == DisciplineFixtures.currentSemesterId })?
                    .disciplines.first,
                forcedMode: .passed
            )
        }
    }

    #Preview("Impossible") {
        NavigationStack {
            FinalCountdownView(
                seed: DisciplineFixtures.semesters
                    .first(where: { $0.id == DisciplineFixtures.currentSemesterId })?
                    .disciplines.first,
                forcedMode: .impossible
            )
        }
    }
#endif
