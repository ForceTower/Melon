import ComposableArchitecture
import SwiftUI

struct MaterialsView: View {
    @Bindable var store: StoreOf<MaterialsFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(Text(.materialsTitle))
        .navigationBarTitleDisplayMode(.large)
        .task { await store.send(.task).finish() }
        .sheet(item: $store.scope(state: \.upload, action: \.upload)) { uploadStore in
            MaterialsUploadSheet(store: uploadStore)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let overview = store.overview {
            hub(overview)
        } else if store.loadFailed {
            MaterialsFailureView { store.send(.retryTapped) }
        } else {
            MaterialsLoadingView()
        }
    }

    private func hub(_ overview: MaterialsOverview) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                header(overview)
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 16)

                MaterialsHubHero(savedCount: overview.savedCount) {
                    store.send(.contributeTapped)
                } onOpenSaved: {
                    store.send(.savedTapped)
                }
                .scaleIn(delay: 0.1, duration: 0.62)
                .padding(.bottom, 24)

                VStack(spacing: 0) {
                    MaterialsSectionHeader(.materialsHubSectionDisciplines)
                    VStack(spacing: 10) {
                        ForEach(overview.disciplines) { discipline in
                            MaterialsDisciplineCard(discipline: discipline) {
                                store.send(.disciplineTapped(discipline))
                            }
                        }
                    }
                }
                .fadeUp(delay: 0.2)
                .padding(.bottom, 20)

                Text(.materialsHubFooter)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .multilineTextAlignment(.center)
                    .lineSpacing(2)
                    .padding(.horizontal, 24)
                    .fadeUp(delay: 0.3)
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .refreshable { await store.send(.refreshPulled).finish() }
    }

    private func header(_ overview: MaterialsOverview) -> some View {
        Text(.materialsHubSubtitle(
            String.localized(overview.totalCount == 1
                ? .materialsCountOne(overview.totalCount)
                : .materialsCountOther(overview.totalCount)),
            overview.disciplines.count,
            overview.semester
        ))
        .font(.system(size: 15, weight: .medium))
        .tracking(-0.15)
        .foregroundStyle(UNESColor.ink3)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
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

// MARK: - Hero

/// The dark mesh hero selling contribution, with the saved-shelf chip once
/// anything is bookmarked.
private struct MaterialsHubHero: View {
    var savedCount: Int
    var onContribute: () -> Void
    var onOpenSaved: () -> Void

    var body: some View {
        ZStack(alignment: .topLeading) {
            MeshView(variant: .warm, intensity: 1)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.plum.opacity(0.34), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.68), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 10, weight: .semibold))
                    Text(.materialsHubBadge)
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .bold))
                        .tracking(0.5)
                }
                .foregroundStyle(.white)
                .padding(EdgeInsets(top: 4, leading: 10, bottom: 4, trailing: 10))
                .background(.white.opacity(0.16), in: Capsule())

                Text(.materialsHubHeroTitle)
                    .font(.system(size: 23, weight: .bold))
                    .tracking(-0.69)
                    .lineSpacing(2)
                    .foregroundStyle(.white)
                    .padding(.top, 13)

                Text(.materialsHubHeroSubtitle)
                    .font(.system(size: 13.5, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(.white.opacity(0.72))
                    .padding(.top, 8)

                HStack(spacing: 10) {
                    Button(action: onContribute) {
                        HStack(spacing: 7) {
                            Image(systemName: "plus")
                                .font(.system(size: 13, weight: .bold))
                            Text(.materialsHubContribute)
                                .font(.system(size: 15, weight: .semibold))
                                .tracking(-0.15)
                        }
                        .foregroundStyle(UNESColor.darkBg)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(.white, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(TilePressStyle())

                    if savedCount > 0 {
                        Button(action: onOpenSaved) {
                            HStack(spacing: 7) {
                                Image(systemName: "bookmark.fill")
                                    .font(.system(size: 12, weight: .semibold))
                                Text(String(savedCount))
                                    .font(.system(size: 15, weight: .semibold))
                                    .monospacedDigit()
                            }
                            .foregroundStyle(.white)
                            .padding(.horizontal, 16)
                            .frame(height: 46)
                            .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .strokeBorder(.white.opacity(0.22))
                            }
                        }
                        .buttonStyle(TilePressStyle())
                    }
                }
                .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .background(UNESColor.darkBg)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.26), radius: 20, y: 9)
    }
}

// MARK: - Discipline card

private struct MaterialsDisciplineCard: View {
    var discipline: MaterialsDiscipline
    var onOpen: () -> Void

    private var tone: Color { UNESColor.disciplineReadableColor(discipline.colorIndex) }

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .center, spacing: 14) {
                Text(discipline.code)
                    .font(.system(size: 11, weight: .heavy))
                    .tracking(0.2)
                    .foregroundStyle(tone)
                    .minimumScaleFactor(0.7)
                    .lineLimit(1)
                    .padding(.horizontal, 4)
                    .frame(width: 46, height: 46)
                    .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 13, style: .continuous))

                VStack(alignment: .leading, spacing: 6) {
                    Text(discipline.name)
                        .font(.system(size: 15.5, weight: .semibold))
                        .tracking(-0.31)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    if discipline.total == 0 {
                        Text(.materialsHubEmptyDiscipline)
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    } else {
                        tally
                    }
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(TilePressStyle())
        .materialsCard()
    }

    /// Per-type mini counts with colored dots.
    private var tally: some View {
        FlowLayout(spacing: 10, lineSpacing: 4) {
            ForEach(MaterialType.allCases.filter { discipline.count(of: $0) > 0 }, id: \.self) { type in
                HStack(spacing: 5) {
                    Circle()
                        .fill(type.tone)
                        .frame(width: 7, height: 7)
                    Text(tallyLabel(type: type, count: discipline.count(of: type)))
                        .font(.system(size: 12, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
    }

    private func tallyLabel(type: MaterialType, count: Int) -> String {
        let label = String.localized(count == 1 ? type.label : type.pluralLabel).lowercased()
        return "\(count) \(label)"
    }
}

#Preview("Materiais") {
    NavigationStack {
        MaterialsView(
            store: Store(initialState: MaterialsFeature.State()) {
                MaterialsFeature()
            }
        )
    }
}

#Preview("Falha") {
    NavigationStack {
        MaterialsView(
            store: Store(initialState: MaterialsFeature.State()) {
                MaterialsFeature()
            } withDependencies: {
                $0.materialsRepository.overview = { throw APIError.emptyEnvelope }
            }
        )
    }
}
