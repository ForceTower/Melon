import ComposableArchitecture
import SwiftUI

/// Hands-on workshops, grouped by audience.
@Reducer
struct CampusEventWorkshopsFeature {
    @ObservableState
    struct State: Equatable {
        let workshops: [CampusEventWorkshop]
    }

    enum Action: Equatable {}

    var body: some ReducerOf<Self> {
        EmptyReducer()
    }
}

struct CampusEventWorkshopsView: View {
    let store: StoreOf<CampusEventWorkshopsFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            CampusEventDetailWash(tone: UNESColor.teal)

            ScrollView {
                VStack(spacing: 0) {
                    Text(.campusEventWorkshopsSubtitle)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 0, leading: 4, bottom: 18, trailing: 4))
                        .fadeUp(delay: 0.02)

                    ForEach(Array(groups.enumerated()), id: \.element.audience) { index, group in
                        VStack(spacing: 12) {
                            if groups.count > 1 {
                                CampusEventSectionHeader(sectionTitle(for: group.audience))
                                    .padding(.bottom, -12)
                            }
                            ForEach(group.workshops) { workshop in
                                CampusEventWorkshopCard(workshop: workshop)
                            }
                        }
                        .padding(.bottom, 24)
                        .fadeUp(delay: 0.06 + Double(index) * 0.06)
                    }
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 12, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(.campusEventWorkshopsTitle))
    }

    /// Freshmen first, then veterans, then shared — skipping empty groups.
    private var groups: [(audience: CampusEventAudience, workshops: [CampusEventWorkshop])] {
        [CampusEventAudience.freshmen, .veterans, .everyone].compactMap { audience in
            let workshops = store.workshops.filter { $0.audience == audience }
            return workshops.isEmpty ? nil : (audience, workshops)
        }
    }

    private func sectionTitle(for audience: CampusEventAudience) -> LocalizedStringResource {
        switch audience {
        case .freshmen: .campusEventWorkshopsFreshmen
        case .veterans: .campusEventWorkshopsVeterans
        case .everyone: .campusEventWorkshopsEveryone
        }
    }
}

struct CampusEventWorkshopCard: View {
    let workshop: CampusEventWorkshop

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                Image(systemName: "wrench.and.screwdriver")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(workshop.audience.tone)
                    .frame(width: 40, height: 40)
                    .background(
                        workshop.audience.tone.opacity(0.12),
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )
                Text(workshop.title)
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if let details = workshop.details {
                Text(details)
                    .font(.system(size: 13.5, weight: .medium))
                    .lineSpacing(4)
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 12)
            }

            FlowLayout(spacing: 8) {
                if let instructors = workshop.instructors {
                    metaChip(icon: "person", text: instructors)
                }
                if let venueName = workshop.venueName {
                    metaChip(icon: "mappin.and.ellipse", text: venueName)
                }
            }
            .padding(.top, 14)

            HStack(spacing: 8) {
                signupBadge
                Spacer(minLength: 0)
                CampusEventAudienceChip(audience: workshop.audience)
            }
            .padding(.top, 12)
            .overlay(alignment: .top) {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
            }
            .padding(.top, 12)
        }
        .padding(15)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(alignment: .topTrailing) {
            Circle()
                .fill(workshop.audience.tone.opacity(0.1))
                .frame(width: 96, height: 96)
                .offset(x: 30, y: -30)
        }
        .campusEventCard()
    }

    private func metaChip(icon: String, text: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
            Text(text)
                .font(.system(size: 12.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
        }
        .padding(EdgeInsets(top: 6, leading: 10, bottom: 6, trailing: 10))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    @ViewBuilder
    private var signupBadge: some View {
        if workshop.requiresSignup {
            HStack(spacing: 5) {
                Image(systemName: "ticket")
                    .font(.system(size: 11, weight: .semibold))
                if let slots = workshop.slots {
                    Text(.campusEventWorkshopsSignupSlots(slots))
                        .font(.system(size: 12, weight: .bold))
                } else {
                    Text(.campusEventWorkshopsSignup)
                        .font(.system(size: 12, weight: .bold))
                }
            }
            .foregroundStyle(UNESColor.caution)
            .padding(EdgeInsets(top: 5, leading: 11, bottom: 5, trailing: 11))
            .background(UNESColor.amber.opacity(0.12), in: Capsule())
        } else {
            HStack(spacing: 5) {
                Image(systemName: "checkmark")
                    .font(.system(size: 10, weight: .bold))
                Text(.campusEventWorkshopsAllIn)
                    .font(.system(size: 12, weight: .bold))
            }
            .foregroundStyle(UNESColor.successGreen)
            .padding(EdgeInsets(top: 5, leading: 11, bottom: 5, trailing: 11))
            .background(UNESColor.successGreen.opacity(0.12), in: Capsule())
        }
    }
}

#Preview("Oficinas") {
    NavigationStack {
        CampusEventWorkshopsView(
            store: Store(
                initialState: CampusEventWorkshopsFeature.State(workshops: CampusEvent.preview().workshops)
            ) {
                CampusEventWorkshopsFeature()
            }
        )
    }
}
