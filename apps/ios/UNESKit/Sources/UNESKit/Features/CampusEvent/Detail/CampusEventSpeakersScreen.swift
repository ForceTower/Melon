import ComposableArchitecture
import SwiftUI

/// The guest list. Rows expand in place to show the bio, so the only state
/// is which card is open — kept in the view.
@Reducer
struct CampusEventSpeakersFeature {
    @ObservableState
    struct State: Equatable {
        let speakers: [CampusEventSpeaker]
    }

    enum Action: Equatable {
        case task
    }

    @Dependency(\.analytics) var analytics

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .task:
                analytics.screen(Screens.campusEventSpeakers)
                return .none
            }
        }
    }
}

struct CampusEventSpeakersView: View {
    let store: StoreOf<CampusEventSpeakersFeature>

    @State private var expandedId: CampusEventSpeaker.ID?

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            CampusEventDetailWash(tone: UNESColor.violet)

            ScrollView {
                VStack(spacing: 10) {
                    Text(.campusEventSpeakersSubtitle)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 0, leading: 4, bottom: 8, trailing: 4))
                        .fadeUp(delay: 0.02)

                    ForEach(Array(store.speakers.enumerated()), id: \.element.id) { index, speaker in
                        speakerCard(speaker)
                            .fadeUp(delay: 0.04 + Double(index) * 0.03)
                    }
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(.campusEventSpeakersTitle))
        .task { await store.send(.task).finish() }
    }

    private func speakerCard(_ speaker: CampusEventSpeaker) -> some View {
        let isExpanded = expandedId == speaker.id
        return Button {
            withAnimation(UNESMotion.ease(0.35)) {
                expandedId = isExpanded ? nil : speaker.id
            }
        } label: {
            VStack(spacing: 0) {
                HStack(spacing: 13) {
                    CampusEventAvatar(name: speaker.name, size: 46)
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 7) {
                            Text(speaker.name)
                                .font(.system(size: 15.5, weight: .semibold))
                                .tracking(-0.31)
                                .foregroundStyle(UNESColor.ink)
                                .lineLimit(1)
                            if let tag = speaker.tag {
                                Text(tag)
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(UNESColor.violet)
                                    .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                                    .background(
                                        UNESColor.violet.opacity(0.1),
                                        in: RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    )
                            }
                        }
                        Text(subtitle(for: speaker))
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    if speaker.bio != nil {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                            .rotationEffect(.degrees(isExpanded ? 90 : 0))
                    }
                }
                .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 14))

                if isExpanded, let bio = speaker.bio {
                    Text(bio)
                        .font(.system(size: 14, weight: .medium))
                        .lineSpacing(5)
                        .foregroundStyle(UNESColor.ink2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 12, leading: 15, bottom: 15, trailing: 15))
                        .overlay(alignment: .top) {
                            Rectangle()
                                .fill(UNESColor.line)
                                .frame(height: 0.5)
                        }
                }
            }
            .campusEventCard()
        }
        .buttonStyle(.plain)
        .disabled(speaker.bio == nil)
    }

    private func subtitle(for speaker: CampusEventSpeaker) -> String {
        [speaker.role, speaker.organization].compactMap(\.self).joined(separator: " · ")
    }
}

#Preview("Convidados") {
    NavigationStack {
        CampusEventSpeakersView(
            store: Store(
                initialState: CampusEventSpeakersFeature.State(speakers: CampusEvent.preview().speakers)
            ) {
                CampusEventSpeakersFeature()
            }
        )
    }
}
