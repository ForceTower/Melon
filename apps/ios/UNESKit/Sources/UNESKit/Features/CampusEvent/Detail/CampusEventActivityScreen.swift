import ComposableArchitecture
import SwiftUI

/// One schedule activity: category hero, description, signup notice, hosts
/// and the venue link.
@Reducer
struct CampusEventActivityFeature {
    @ObservableState
    struct State: Equatable {
        let activity: CampusEventActivity
        let event: CampusEvent
    }

    enum Action: Equatable {
        case venueTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openVenues(CampusEvent)
        }
    }

    private let log = Log.scoped("CampusEventActivityFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .venueTapped:
                log.info("open venues from activity id=\(state.activity.id)")
                return .send(.delegate(.openVenues(state.event)))
            case .delegate:
                return .none
            }
        }
    }
}

struct CampusEventActivityView: View {
    let store: StoreOf<CampusEventActivityFeature>

    private var activity: CampusEventActivity { store.activity }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            CampusEventDetailWash(tone: activity.category.tone)

            ScrollView {
                VStack(spacing: 16) {
                    hero
                        .scaleIn(delay: 0.02, duration: 0.62)

                    if let details = activity.details {
                        Text(details)
                            .font(.system(size: 15.5, weight: .medium))
                            .lineSpacing(6)
                            .foregroundStyle(UNESColor.ink2)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16))
                            .campusEventCard()
                            .fadeUp(delay: 0.1)
                    }

                    if activity.requiresSignup {
                        signupNotice
                            .fadeUp(delay: 0.14)
                    }

                    if !activity.speakerNames.isEmpty {
                        speakers
                            .fadeUp(delay: 0.18)
                    }

                    venue
                        .fadeUp(delay: 0.22)
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(activity.category.label))
        .inlineNavigationBar()
    }

    // MARK: Hero

    private var hero: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: activity.category.mesh)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.14), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.6), location: 1),
                ],
                angle: 160
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    HStack(spacing: 5) {
                        Image(systemName: activity.category.icon)
                            .font(.system(size: 10, weight: .bold))
                        Text(activity.category.label)
                            .font(.system(size: 11.5, weight: .bold))
                    }
                    .foregroundStyle(.white)
                    .padding(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 10))
                    .background(.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                    CampusEventAudienceChip(audience: activity.audience, large: true)
                }

                Text(activity.title)
                    .font(.system(size: 25, weight: .bold))
                    .tracking(-0.75)
                    .lineSpacing(2)
                    .foregroundStyle(.white)
                    .padding(.top, 14)

                HStack(spacing: 18) {
                    heroMeta(
                        icon: "clock",
                        label: .campusEventDetailTime,
                        value: "\(CampusEventFormat.weekdayShort(for: activity.startsAt, in: store.event.timeZone)) · \(CampusEventFormat.timeRange(from: activity.startsAt, to: activity.endsAt, in: store.event.timeZone))"
                    )
                    heroMeta(icon: "mappin.and.ellipse", label: .campusEventDetailVenue, value: activity.venueName)
                }
                .padding(.top, 15)
                .overlay(alignment: .top) {
                    Rectangle()
                        .fill(.white.opacity(0.16))
                        .frame(height: 1)
                }
                .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: activity.category.tone.opacity(0.27), radius: 20, y: 16)
    }

    private func heroMeta(icon: String, label: LocalizedStringResource, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(.white.opacity(0.7))
                Text(label)
                    .textCase(.uppercase)
                    .font(.system(size: 10.5, weight: .semibold))
                    .tracking(0.53)
                    .foregroundStyle(.white.opacity(0.55))
            }
            Text(value)
                .font(.system(size: 14.5, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(.white)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Sections

    private var signupNotice: some View {
        HStack(spacing: 12) {
            Image(systemName: "ticket")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(UNESColor.caution)
                .frame(width: 34, height: 34)
                .background(UNESColor.amber.opacity(0.15), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
            Text(.campusEventDetailSignup)
                .font(.system(size: 13.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        .background(UNESColor.amber.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.amber.opacity(0.2))
        }
    }

    private var speakers: some View {
        VStack(spacing: 0) {
            CampusEventSectionHeader(
                activity.speakerNames.count > 1 ? .campusEventDetailSpeakersMany : .campusEventDetailSpeakersOne
            )
            VStack(spacing: 0) {
                ForEach(Array(activity.speakerNames.enumerated()), id: \.element) { index, name in
                    let speaker = store.event.speaker(for: activity, at: index)
                    HStack(spacing: 13) {
                        CampusEventAvatar(name: name)
                        VStack(alignment: .leading, spacing: 1) {
                            Text(name)
                                .font(.system(size: 15, weight: .semibold))
                                .tracking(-0.3)
                                .foregroundStyle(UNESColor.ink)
                            if let role = speaker?.role {
                                Text(role)
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundStyle(UNESColor.ink3)
                            } else {
                                Text(.campusEventDetailSpeakerFallback)
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundStyle(UNESColor.ink3)
                            }
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
                    .overlay(alignment: .bottom) {
                        if index < activity.speakerNames.count - 1 {
                            Rectangle()
                                .fill(UNESColor.line)
                                .frame(height: 0.5)
                                .padding(.leading, 69)
                        }
                    }
                }
            }
            .campusEventCard()
        }
    }

    private var venue: some View {
        let venue = store.event.venue(for: activity)
        let tone = venue.map { campusEventTone(for: $0.name) } ?? UNESColor.coral
        return VStack(spacing: 0) {
            CampusEventSectionHeader(.campusEventDetailWhere)
            Button {
                store.send(.venueTapped)
            } label: {
                HStack(spacing: 13) {
                    Image(systemName: "mappin.and.ellipse")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(tone)
                        .frame(width: 42, height: 42)
                        .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                    VStack(alignment: .leading, spacing: 1) {
                        Text(activity.venueName)
                            .font(.system(size: 15, weight: .semibold))
                            .tracking(-0.3)
                            .foregroundStyle(UNESColor.ink)
                        Group {
                            if let hint = venue?.hint {
                                Text(hint)
                            } else {
                                Text(.campusEventDetailWhereHint)
                            }
                        }
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                    }
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                }
                .padding(15)
                .campusEventCard()
            }
            .buttonStyle(.pressableCard)
        }
    }
}

#Preview("Atividade") {
    let event = CampusEvent.preview()
    return NavigationStack {
        CampusEventActivityView(
            store: Store(
                initialState: CampusEventActivityFeature.State(activity: event.activities[4], event: event)
            ) {
                CampusEventActivityFeature()
            }
        )
    }
}
