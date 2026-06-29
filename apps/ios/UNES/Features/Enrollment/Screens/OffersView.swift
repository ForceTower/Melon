import SwiftUI

// UNES — offered-disciplines catalogue: search + mandatory/optional filter,
// grouped by curriculum period, with the running-proposal dock pinned to the
// bottom. Ported from `OffersScreen` in `screens-matricula-screens.jsx`.
struct OffersView: View {
    let enroll: EnrollmentState
    let window: EnrollmentWindow
    let disciplines: [OfferedDiscipline]
    let onOpenDiscipline: (OfferedDiscipline) -> Void
    let onTimetable: () -> Void
    let onReview: () -> Void

    private enum Filter: CaseIterable {
        case all, mandatory, optional
        var label: String {
            switch self {
            case .all: return "Todas"
            case .mandatory: return "Obrigatórias"
            case .optional: return "Optativas"
            }
        }
    }

    @State private var query = ""
    @State private var filter: Filter = .all

    private var filtered: [OfferedDiscipline] {
        var list = disciplines
        switch filter {
        case .mandatory: list = list.filter(\.mandatory)
        case .optional:  list = list.filter { !$0.mandatory }
        case .all:       break
        }
        let term = query.trimmingCharacters(in: .whitespaces).lowercased()
        if !term.isEmpty {
            list = list.filter {
                $0.code.lowercased().contains(term) || $0.name.lowercased().contains(term)
            }
        }
        return list
    }

    var body: some View {
        VStack(spacing: 0) {
            filters
            Rectangle().fill(UNESColor.line).frame(height: 1)
            listBody
        }
        .background(UNESColor.surface.ignoresSafeArea())
        .overlay(alignment: .bottom) {
            EnrollmentDock(
                enroll: enroll,
                window: window,
                secondary: .init(label: "Grade", systemImage: "square.grid.2x2", action: onTimetable),
                primaryLabel: "Revisar",
                primaryAction: onReview
            )
        }
        .navigationTitle("Disciplinas ofertadas")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: Search + filter

    private var filters: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundStyle(UNESColor.ink3)
                TextField("Buscar por código ou nome…", text: $query)
                    .font(UNESFont.sans(13.5))
                    .foregroundStyle(UNESColor.ink)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(UNESColor.surface2)
                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(UNESColor.cardLine, lineWidth: 1))
            )

            HStack(spacing: 6) {
                ForEach(Filter.allCases, id: \.self) { f in
                    Button { filter = f } label: {
                        Text(f.label)
                            .font(UNESFont.sans(12, weight: .semibold))
                            .foregroundStyle(filter == f ? UNESColor.surface : UNESColor.ink3)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 11, style: .continuous)
                                    .fill(filter == f ? UNESColor.ink : UNESColor.card)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 11, style: .continuous)
                                            .strokeBorder(filter == f ? .clear : UNESColor.cardLine, lineWidth: 1)
                                    )
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 6)
        .padding(.bottom, 12)
    }

    // MARK: List

    @ViewBuilder
    private var listBody: some View {
        let sections = EnrollmentScheduling.byPeriod(filtered)
        ScrollView(showsIndicators: false) {
            if sections.isEmpty {
                emptyState
            } else {
                VStack(alignment: .leading, spacing: 22) {
                    ForEach(sections, id: \.period) { section in
                        VStack(alignment: .leading, spacing: 10) {
                            periodHeader(period: section.period, count: section.items.count)
                            ForEach(section.items) { discipline in
                                OfferRow(discipline: discipline, enroll: enroll) {
                                    onOpenDiscipline(discipline)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 14)
                .padding(.bottom, 124)
            }
        }
    }

    private func periodHeader(period: Int, count: Int) -> some View {
        HStack(spacing: 8) {
            EnrollmentEyebrow(text: period == 0 ? "Optativas / eletivas" : "\(period)º período")
            Text("· \(count)")
                .font(UNESFont.mono(9))
                .foregroundStyle(UNESColor.ink4)
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("◦").font(UNESFont.serif(30)).foregroundStyle(UNESColor.ink4)
            Text("Nenhuma disciplina para \"\(query)\"")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 60)
    }
}

#Preview {
    NavigationStack {
        OffersView(
            enroll: .previewSeeded,
            window: EnrollmentFixtures.window,
            disciplines: EnrollmentFixtures.disciplines,
            onOpenDiscipline: { _ in }, onTimetable: {}, onReview: {}
        )
    }
}
