import SwiftUI

/// Collapsible summary card for a past semester. Tap to expand a stack of
/// `PastDisciplineRow`s that each link into the detail screen.
struct PastSemesterCard: View {
    let semesterId: String
    let disciplines: [Discipline]
    var defaultOpen: Bool = false
    let onOpen: (Discipline) -> Void

    @State private var expanded: Bool

    init(semesterId: String, disciplines: [Discipline], defaultOpen: Bool = false,
         onOpen: @escaping (Discipline) -> Void) {
        self.semesterId = semesterId
        self.disciplines = disciplines
        self.defaultOpen = defaultOpen
        self.onOpen = onOpen
        _expanded = State(initialValue: defaultOpen)
    }

    private var finals: [Double] { disciplines.compactMap(\.finalGrade) }

    private var mean: Double? {
        guard !finals.isEmpty else { return nil }
        return finals.reduce(0, +) / Double(finals.count)
    }

    private var approved: Int {
        disciplines.filter { ($0.finalGrade ?? 0) >= 7 }.count
    }

    var body: some View {
        VStack(spacing: 8) {
            header
            if expanded {
                VStack(spacing: 8) {
                    ForEach(disciplines) { d in
                        PastDisciplineRow(discipline: d, onOpen: { onOpen(d) })
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    private var header: some View {
        Button {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.82)) {
                expanded.toggle()
            }
        } label: {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 3) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(semesterId)
                            .font(UNESFont.serif(18))
                            .tracking(-0.18)
                            .foregroundStyle(UNESColor.ink)
                        Text("\(disciplines.count) disciplina\(disciplines.count != 1 ? "s" : "")")
                            .font(UNESFont.mono(10))
                            .tracking(0.8)
                            .foregroundStyle(UNESColor.ink4)
                    }

                    HStack(spacing: 8) {
                        if let mean {
                            HStack(spacing: 4) {
                                Text("média")
                                    .foregroundStyle(UNESColor.ink3)
                                Text(String(format: "%.1f", mean))
                                    .font(UNESFont.mono(12, weight: .semibold))
                                    .foregroundStyle(DisciplineScoreColor.color(for: mean))
                            }
                            Text("·")
                                .foregroundStyle(UNESColor.ink3.opacity(0.4))
                        }
                        Text("\(approved)/\(disciplines.count) aprovadas")
                            .foregroundStyle(UNESColor.ink3)
                    }
                    .font(UNESFont.sans(12))
                }

                Spacer(minLength: 6)

                HStack(spacing: 3) {
                    ForEach(Array(disciplines.prefix(5))) { d in
                        Circle()
                            .fill(d.color)
                            .frame(width: 7, height: 7)
                            .opacity((d.finalGrade ?? 0) >= 7 ? 1 : 0.4)
                    }
                }

                Image(systemName: "chevron.down")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .rotationEffect(.degrees(expanded ? 180 : 0))
                    .animation(.spring(response: 0.3, dampingFraction: 0.82), value: expanded)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .cardSurface(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

/// Compact row used inside an expanded past-semester card. Shows the
/// discipline code, title, and final grade with pass/fail indicator.
struct PastDisciplineRow: View {
    let discipline: Discipline
    let onOpen: () -> Void

    private var final: Double? { discipline.finalGrade }
    private var passed: Bool { (final ?? 0) >= 7 }

    var body: some View {
        Button(action: onOpen) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(discipline.fullCode)
                        .font(UNESFont.mono(9, weight: .bold))
                        .tracking(0.9)
                        .foregroundStyle(discipline.color)
                    Text(discipline.title)
                        .font(UNESFont.serif(15))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                }
                .padding(.leading, 6)
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 2) {
                    Text(final.map { String(format: "%.1f", $0) } ?? "—")
                        .font(UNESFont.serif(22))
                        .tracking(-0.44)
                        .foregroundStyle(DisciplineScoreColor.color(for: final))
                    Text(passed ? "APROVADO" : "REPROVADO")
                        .font(UNESFont.mono(8, weight: .semibold))
                        .tracking(0.8)
                        .foregroundStyle(passed
                                         ? Color(red: 0x2A/255, green: 0x7E/255, blue: 0x8B/255)
                                         : Color(red: 0xB8/255, green: 0x46/255, blue: 0x3A/255))
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background {
                let shape = RoundedRectangle(cornerRadius: 16, style: .continuous)
                ZStack(alignment: .leading) {
                    if #available(iOS 26.0, *) {
                        shape
                            .fill(Color.clear)
                            .glassEffect(.regular.tint(UNESColor.card), in: shape)
                    } else {
                        shape.fill(UNESColor.card)
                    }
                    Rectangle()
                        .fill(discipline.color)
                        .opacity(passed ? 1 : 0.4)
                        .frame(width: 3)
                        .padding(.vertical, 10)
                }
                .clipShape(shape)
                .overlay(shape.strokeBorder(UNESColor.cardLine, lineWidth: 1))
            }
        }
        .buttonStyle(.plain)
    }
}

/// Dashed placeholder card for a semester whose data hasn't been fetched yet.
/// Tapping starts a fake fetch; after a short delay `onDownload(semesterId)`
/// fires so the parent can swap in the real list.
struct UndownloadedSemesterCard: View {
    let semesterId: String
    let estimatedCount: Int?
    let onDownload: (String) -> Void

    private enum LoadState { case idle, loading, done }
    @State private var state: LoadState = .idle

    var body: some View {
        Button(action: tap) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(UNESColor.surface2)
                        .frame(width: 40, height: 40)

                    Group {
                        if state == .loading {
                            Image(systemName: "arrow.triangle.2.circlepath")
                                .rotationEffect(.degrees(spinning ? 360 : 0))
                                .onAppear { spinning = true }
                                .animation(.linear(duration: 0.9).repeatForever(autoreverses: false), value: spinning)
                        } else {
                            Image(systemName: "arrow.down.to.line")
                        }
                    }
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                }

                VStack(alignment: .leading, spacing: 3) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(semesterId)
                            .font(UNESFont.serif(18))
                            .tracking(-0.18)
                            .foregroundStyle(state == .loading ? UNESColor.ink3 : UNESColor.ink)
                        Text(countLabel)
                            .font(UNESFont.mono(10))
                            .tracking(0.8)
                            .foregroundStyle(UNESColor.ink4)
                    }

                    Text(state == .loading
                         ? "Buscando notas e faltas do semestre…"
                         : "Toque para baixar o histórico deste semestre.")
                        .font(UNESFont.sans(12))
                        .foregroundStyle(UNESColor.ink3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if state == .idle {
                    Text("BAIXAR")
                        .font(UNESFont.mono(10, weight: .semibold))
                        .tracking(1)
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                        )
                        .fixedSize()
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background {
                // Liquid Glass on iOS 26+, solid fill below — dashed stroke
                // reused on both branches so the "not yet downloaded" affordance
                // stays consistent.
                let shape = RoundedRectangle(cornerRadius: 18, style: .continuous)
                let dashed = RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                    .foregroundStyle(UNESColor.cardLine)
                if #available(iOS 26.0, *) {
                    shape
                        .fill(Color.clear)
                        .glassEffect(.regular.tint(UNESColor.card), in: shape)
                        .overlay(dashed)
                } else {
                    shape
                        .fill(UNESColor.card)
                        .overlay(dashed)
                }
            }
        }
        .buttonStyle(.plain)
        .disabled(state != .idle)
    }

    @State private var spinning = false

    // Subtitle copy: "baixando…" while fetching, a disciplines count when
    // we have one, or a neutral "não baixado" when the count is unknown.
    private var countLabel: String {
        if state == .loading { return "baixando…" }
        guard let count = estimatedCount else { return "não baixado" }
        return "~\(count) disciplina\(count != 1 ? "s" : "")"
    }

    private func tap() {
        guard state == .idle else { return }
        withAnimation(.easeOut(duration: 0.2)) { state = .loading }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) {
            withAnimation(.easeOut(duration: 0.2)) { state = .done }
            onDownload(semesterId)
        }
    }
}
