import SwiftUI

/// Advanced search: up to three scoped terms combined with E / OU / NÃO,
/// plus up-front restrictions — broad queries expire upstream, so narrowing
/// before searching avoids the 30 s wait.
struct LibraryAdvancedSheet: View {
    var onSearch: (String, LibrarySearchScope, LibraryFacetSelection) -> Void

    @Environment(\.dismiss) private var dismiss

    private struct TermRow: Equatable {
        var scope: LibrarySearchScope
        var term = ""
        var op: Operator = .and
    }

    private enum Operator: Equatable, CaseIterable {
        case and, or, not

        var label: LocalizedStringResource {
            switch self {
            case .and: .libraryAdvancedOpAnd
            case .or: .libraryAdvancedOpOr
            case .not: .libraryAdvancedOpNot
            }
        }
    }

    @State private var rows: [TermRow] = [
        TermRow(scope: .title),
        TermRow(scope: .author),
    ]
    @State private var branch: LibraryBranch?
    @State private var type: LibraryWorkType?

    private var filled: [TermRow] {
        rows.filter { !$0.term.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(rows.indices, id: \.self) { index in
                        if index > 0 {
                            Picker(selection: $rows[index].op) {
                                ForEach(Operator.allCases, id: \.self) { op in
                                    Text(op.label).tag(op)
                                }
                            } label: {
                                EmptyView()
                            }
                            .segmentedPickerCompat()
                        }
                        termCard(index)
                    }

                    if rows.count < 3 {
                        Button {
                            withAnimation(UNESMotion.ease(0.25)) {
                                rows.append(TermRow(scope: .subject))
                            }
                        } label: {
                            HStack(spacing: 7) {
                                Image(systemName: "plus")
                                    .font(.system(size: 13, weight: .bold))
                                Text(rows.count == 1 ? .libraryAdvancedAddSecond : .libraryAdvancedAddThird)
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundStyle(UNESColor.ink3)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .background {
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .strokeBorder(UNESColor.line, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }

                    restrictSection
                        .padding(.top, 12)
                }
                .padding(EdgeInsets(top: 8, leading: 18, bottom: 20, trailing: 18))
            }
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.immediately)
            .background(UNESColor.surface)
            .navigationTitle(Text(.libraryAdvancedTitle))
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Text(.libraryAdvancedCancel)
                    }
                    .tint(UNESColor.accent)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        rows = rows.map { row in
                            var row = row
                            row.term = ""
                            return row
                        }
                    } label: {
                        Text(.libraryAdvancedClear)
                    }
                    .tint(UNESColor.ink4)
                }
            }
            .safeAreaInset(edge: .bottom) {
                searchBar
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
    }

    private func termCard(_ index: Int) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 6) {
                // Bleeds through the card padding so scrolled chips reach the
                // card edge, fading out instead of clipping mid-chip.
                ScrollView(.horizontal) {
                    HStack(spacing: 6) {
                        ForEach(LibrarySearchScope.allCases.filter { $0 != .all }, id: \.self) { scope in
                            Button {
                                rows[index].scope = scope
                            } label: {
                                Text(scope.label)
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(rows[index].scope == scope ? UNESColor.ink : UNESColor.ink4)
                                    .padding(.horizontal, 10)
                                    .frame(height: 26)
                                    .background(
                                        rows[index].scope == scope ? UNESColor.surface3 : .clear,
                                        in: RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.leading, 13)
                    .padding(.trailing, index > 0 ? 4 : 13)
                }
                .scrollIndicators(.hidden)
                .chipRowFade(leading: 13, trailing: 24)
                .padding(.leading, -13)
                .padding(.trailing, index > 0 ? 0 : -13)
                if index > 0 {
                    Button {
                        withAnimation(UNESMotion.ease(0.25)) {
                            _ = rows.remove(at: index)
                        }
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(UNESColor.ink3)
                            .frame(width: 26, height: 26)
                            .background(UNESColor.surface2, in: Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(.libraryAdvancedRemove))
                }
            }
            TextField(
                "",
                text: $rows[index].term,
                prompt: Text(.libraryAdvancedTermPlaceholder)
            )
            .font(.system(size: 17, weight: .medium))
            .foregroundStyle(UNESColor.ink)
            .autocorrectionDisabled()
            .noAutocapitalization()
            .submitLabel(.search)
            .onSubmit(submit)
        }
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 13, trailing: 13))
        .materialsCard(cornerRadius: 16)
    }

    private var restrictSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            MaterialsSectionHeader(.libraryAdvancedRestrictTitle)
            Text(.libraryAdvancedRestrictHint)
                .font(.system(size: 12.5, weight: .medium))
                .lineSpacing(2)
                .foregroundStyle(UNESColor.ink4)
                .padding(EdgeInsets(top: -4, leading: 4, bottom: 11, trailing: 4))

            VStack(alignment: .leading, spacing: 9) {
                chipStrip {
                    ForEach(LibraryBranch.allCases, id: \.self) { option in
                        LibraryChip(isActive: branch == option, onTap: {
                            branch = branch == option ? nil : option
                        }) {
                            Text(option.sigla)
                        }
                    }
                }
                chipStrip {
                    ForEach(
                        [LibraryWorkType.book, .dissertation, .cordel, .article],
                        id: \.self
                    ) { option in
                        LibraryChip(
                            isActive: type == option,
                            tone: option.accent ?? UNESColor.ink,
                            onTap: { type = type == option ? nil : option }
                        ) {
                            Text(option.pluralLabel)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func chipStrip(@ViewBuilder content: () -> some View) -> some View {
        ScrollView(.horizontal) {
            HStack(spacing: 7, content: content)
                .padding(.horizontal, 18)
                .padding(.vertical, 14)
        }
        .scrollIndicators(.hidden)
        .chipRowFade(leading: 18, trailing: 24)
        .padding(.horizontal, -18)
        .padding(.vertical, -14)
    }

    private var searchBar: some View {
        Button(action: submit) {
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 14, weight: .bold))
                Text(searchLabel)
                    .tracking(-0.17)
            }
        }
        .buttonStyle(.unesAccent)
        .disabled(filled.isEmpty)
        .padding(EdgeInsets(top: 10, leading: 20, bottom: 8, trailing: 20))
        .background(UNESColor.surface)
    }

    private var searchLabel: String {
        switch filled.count {
        case 0: .localized(.libraryAdvancedSearchCta)
        case 1: .localized(.libraryAdvancedSearchCtaOne(1))
        default: .localized(.libraryAdvancedSearchCtaOther(filled.count))
        }
    }

    private func submit() {
        let terms = filled
        guard !terms.isEmpty else { return }
        var facets: LibraryFacetSelection = [:]
        if let branch { facets[.branch] = [branch.rawValue] }
        if let type { facets[.type] = [type.rawValue] }
        onSearch(
            terms.map { $0.term.trimmingCharacters(in: .whitespaces) }.joined(separator: " "),
            terms[0].scope,
            facets
        )
    }
}

#Preview("Busca avançada") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            LibraryAdvancedSheet { _, _, _ in }
        }
}
