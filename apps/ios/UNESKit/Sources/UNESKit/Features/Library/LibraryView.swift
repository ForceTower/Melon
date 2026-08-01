import ComposableArchitecture
import SwiftUI

struct LibraryView: View {
    @Bindable var store: StoreOf<LibraryFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(Text(.libraryTitle))
        .largeNavigationBar()
        .task { await store.send(.task).finish() }
        .sheet(isPresented: $store.isAdvancedPresented) {
            LibraryAdvancedSheet { terms, facets in
                store.send(.advancedSubmitted(terms: terms, facets: facets))
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.overview != nil {
            hub
        } else if store.loadFailed {
            MaterialsFailureView { store.send(.retryTapped) }
        } else {
            MaterialsLoadingView()
        }
    }

    private var hub: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 16)

                SearchField(
                    placeholder: .libraryHubSearchPlaceholder,
                    query: store.query,
                    onQueryChange: { store.send(.queryChanged($0)) }
                )
                .onSubmit { store.send(.submitTapped) }
                .submitLabel(.search)
                .fadeUp(delay: 0.08)
                .padding(.bottom, 12)

                scopeChips
                    .fadeUp(delay: 0.12)
                    .padding(.bottom, 14)

                advancedCard
                    .fadeUp(delay: 0.16)
                    .padding(.bottom, 24)

                if !store.recents.isEmpty {
                    recentsSection
                        .fadeUp(delay: 0.2)
                        .padding(.bottom, 24)
                }

                newAcquisitionsSection
                    .fadeUp(delay: 0.24)
                    .padding(.bottom, 20)

                LibraryNote(icon: "book", .libraryHubCatalogueNote)
                    .fadeUp(delay: 0.28)
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.immediately)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            Eyebrow(text: .localized(.libraryHubEyebrow))
            Text(.libraryHubSubtitle)
                .font(.system(size: 15, weight: .medium))
                .tracking(-0.15)
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var scopeChips: some View {
        LibraryChipRow {
            ForEach(LibrarySearchScope.allCases, id: \.self) { scope in
                LibraryChip(isActive: store.searchScope == scope, onTap: { store.send(.scopeTapped(scope)) }) {
                    Text(scope.label)
                }
            }
        }
    }

    private var advancedCard: some View {
        Button {
            store.send(.advancedTapped)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.ink2)
                    .frame(width: 34, height: 34)
                    .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                VStack(alignment: .leading, spacing: 1) {
                    Text(.libraryHubAdvancedTitle)
                        .font(.system(size: 14.5, weight: .semibold))
                        .tracking(-0.29)
                        .foregroundStyle(UNESColor.ink)
                    Text(.libraryHubAdvancedSubtitle)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
        .materialsCard()
    }

    // MARK: Recents

    private var recentsSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(title: .libraryHubSectionRecents) {
                Button {
                    store.send(.clearRecentsTapped)
                } label: {
                    Text(.libraryHubRecentsClear)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.accent)
                }
                .buttonStyle(.plain)
            }
            VStack(spacing: 0) {
                ForEach(Array(store.recents.enumerated()), id: \.element.id) { index, recent in
                    if index > 0 {
                        Divider()
                            .overlay(UNESColor.line)
                            .padding(.leading, 15)
                    }
                    recentRow(recent)
                }
            }
            .materialsCard()
        }
    }

    private func recentRow(_ recent: LibraryRecentSearch) -> some View {
        Button {
            store.send(.recentTapped(recent))
        } label: {
            HStack(spacing: 11) {
                Image(systemName: "clock")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                VStack(alignment: .leading, spacing: 2) {
                    Text(recent.query)
                        .font(.system(size: 14.5, weight: .medium))
                        .tracking(-0.29)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    if recent.scope != .all {
                        Text(.libraryHubRecentScope(String.localized(recent.scope.label).lowercased()))
                            .font(.system(size: 11.5, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                Spacer(minLength: 8)
                Text(LibraryFormat.count(recent.resultCount))
                    .font(.system(size: 12.5, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 12, leading: 15, bottom: 12, trailing: 15))
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
    }

    // MARK: Novas no acervo

    private var newAcquisitionsSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.libraryHubSectionNew)
            ScrollView(.horizontal) {
                // Every card stretches to the tallest in the row, so the shelf
                // reads as a uniform rack even when titles wrap differently.
                HStack(alignment: .top, spacing: 11) {
                    ForEach(store.overview?.newAcquisitions ?? []) { work in
                        LibraryNewAcquisitionCard(work: work) {
                            store.send(.newAcquisitionTapped(work))
                        }
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 20)
            }
            .scrollIndicators(.hidden)
            .scrollClipDisabled()
            .padding(.horizontal, -16)
            .padding(.bottom, 12)

            LibraryNote(.libraryHubNewNote)
        }
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
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
            .opacity(0.26)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

extension LibrarySearchScope {
    var label: LocalizedStringResource {
        switch self {
        case .all: .libraryScopeAll
        case .title: .libraryScopeTitle
        case .author: .libraryScopeAuthor
        case .subject: .libraryScopeSubject
        case .isbn: .libraryScopeIsbn
        case .callNumber: .libraryScopeCallNumber
        }
    }
}

// MARK: - New acquisition card

/// The degraded "novas no acervo" shape: one author line, no copy counts —
/// availability only gets consulted once the work is opened.
struct LibraryNewAcquisitionCard: View {
    var work: LibraryWork
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 11) {
                    LibraryWorkMark(work: work, width: 44, height: 64)
                    VStack(alignment: .leading, spacing: 5) {
                        LibraryTypeTag(type: work.type, size: 9.5)
                        Text(work.parsedTitle.title)
                            .font(.system(size: 14, weight: .semibold))
                            .tracking(-0.35)
                            .lineSpacing(1)
                            .foregroundStyle(UNESColor.ink)
                            .lineLimit(3)
                            .multilineTextAlignment(.leading)
                    }
                }
                // Pinned to the bottom so author and CTA line up across the
                // row no matter how many lines each title takes.
                Spacer(minLength: 0)
                Text(work.authors.first ?? .localized(.libraryAuthorUnknown))
                    .font(.system(size: 11.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    Image(systemName: "books.vertical")
                        .font(.system(size: 11, weight: .semibold))
                    Text(.libraryHubNewCardCta)
                        .font(.system(size: 11.5, weight: .semibold))
                }
                .foregroundStyle(UNESColor.accent)
            }
            .padding(13)
            .frame(width: 208, alignment: .topLeading)
            .frame(maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
        .materialsCard()
    }
}

#Preview("Biblioteca") {
    NavigationStack {
        LibraryView(
            store: Store(initialState: LibraryFeature.State()) {
                LibraryFeature()
            }
        )
    }
}
