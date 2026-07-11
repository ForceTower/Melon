import ComposableArchitecture
import SwiftUI

/// Where the event happens: a schematic campus map (when the venues carry
/// coordinates) over the venue list. Pin selection is view-only state.
@Reducer
struct CampusEventVenuesFeature {
    @ObservableState
    struct State: Equatable {
        let event: CampusEvent

        var venues: [CampusEventVenue] { event.venues }
    }

    enum Action: Equatable {}

    var body: some ReducerOf<Self> {
        EmptyReducer()
    }
}

struct CampusEventVenuesView: View {
    let store: StoreOf<CampusEventVenuesFeature>

    @State private var selectedId: CampusEventVenue.ID?

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            CampusEventDetailWash(tone: UNESColor.amber)

            ScrollView {
                VStack(spacing: 0) {
                    Text(.campusEventVenuesSubtitle)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 0, leading: 4, bottom: 16, trailing: 4))
                        .fadeUp(delay: 0.02)

                    if !mappedVenues.isEmpty {
                        map
                            .scaleIn(delay: 0.04, duration: 0.62)
                            .padding(.bottom, 18)
                    }

                    CampusEventSectionHeader(.campusEventVenuesSpaces)
                        .fadeUp(delay: 0.1)
                    venueList
                        .fadeUp(delay: 0.12)
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(.campusEventVenuesTitle))
    }

    private var mappedVenues: [CampusEventVenue] {
        store.venues.filter { $0.mapX != nil && $0.mapY != nil }
    }

    private func tone(for venue: CampusEventVenue) -> Color {
        let index = store.venues.firstIndex { $0.id == venue.id } ?? 0
        return campusEventPalette[index % campusEventPalette.count]
    }

    // MARK: Schematic map

    private var map: some View {
        GeometryReader { proxy in
            ZStack {
                mapBackdrop

                ForEach(mappedVenues) { venue in
                    pin(for: venue)
                        .position(
                            x: proxy.size.width * (venue.mapX ?? 0) / 100,
                            y: proxy.size.height * (venue.mapY ?? 0) / 100
                        )
                }
            }
        }
        .frame(height: 268)
        .background(
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.surface2, location: 0),
                    .init(color: UNESColor.surface3, location: 1),
                ],
                angle: 160
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.08), radius: 12, y: 8)
    }

    /// Blueprint grid plus a few soft "building" blocks — decorative only;
    /// the pins carry the information.
    private var mapBackdrop: some View {
        ZStack {
            Canvas { context, size in
                var grid = Path()
                for x in stride(from: CGFloat.zero, through: size.width, by: 40) {
                    grid.move(to: CGPoint(x: x, y: 0))
                    grid.addLine(to: CGPoint(x: x, y: size.height))
                }
                for y in stride(from: CGFloat.zero, through: size.height, by: 40) {
                    grid.move(to: CGPoint(x: 0, y: y))
                    grid.addLine(to: CGPoint(x: size.width, y: y))
                }
                context.stroke(grid, with: .color(UNESColor.line.opacity(0.5)), lineWidth: 1)
            }

            GeometryReader { proxy in
                let blocks: [(x: Double, y: Double, w: Double, h: Double)] = [
                    (8, 14, 38, 30), (56, 10, 34, 40), (12, 52, 32, 34), (58, 50, 34, 38),
                ]
                ForEach(Array(blocks.enumerated()), id: \.offset) { _, block in
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(UNESColor.card.opacity(0.55))
                        .frame(
                            width: proxy.size.width * block.w / 100,
                            height: proxy.size.height * block.h / 100
                        )
                        .offset(
                            x: proxy.size.width * block.x / 100,
                            y: proxy.size.height * block.y / 100
                        )
                }
            }
        }
        .allowsHitTesting(false)
    }

    private func pin(for venue: CampusEventVenue) -> some View {
        let isSelected = selectedId == venue.id
        let tone = tone(for: venue)
        return Button {
            withAnimation(UNESMotion.ease(0.3, overshoot: 1.2)) {
                selectedId = isSelected ? nil : venue.id
            }
        } label: {
            VStack(spacing: 6) {
                if isSelected {
                    Text(venue.displayShortName)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(UNESColor.surface)
                        .padding(EdgeInsets(top: 5, leading: 9, bottom: 5, trailing: 9))
                        .background(UNESColor.ink, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .fixedSize()
                        .shadow(color: .black.opacity(0.25), radius: 8, y: 5)
                        .transition(.scale(scale: 0.7).combined(with: .opacity))
                }
                Image(systemName: "mappin")
                    .font(.system(size: isSelected ? 14 : 11, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: isSelected ? 30 : 24, height: isSelected ? 30 : 24)
                    .background(tone, in: Circle())
                    .overlay {
                        Circle().strokeBorder(UNESColor.card, lineWidth: 2.5)
                    }
                    .shadow(color: tone.opacity(0.4), radius: 8, y: 5)
            }
        }
        .buttonStyle(.plain)
        .zIndex(isSelected ? 1 : 0)
    }

    // MARK: Venue list

    private var venueList: some View {
        VStack(spacing: 0) {
            ForEach(Array(store.venues.enumerated()), id: \.element.id) { index, venue in
                let count = store.event.activityCount(at: venue)
                Button {
                    withAnimation(UNESMotion.ease(0.3, overshoot: 1.2)) {
                        selectedId = venue.id
                    }
                } label: {
                    HStack(spacing: 13) {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(tone(for: venue))
                            .frame(width: 40, height: 40)
                            .background(
                                tone(for: venue).opacity(0.13),
                                in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                            )
                        VStack(alignment: .leading, spacing: 1) {
                            Text(venue.name)
                                .font(.system(size: 15, weight: .semibold))
                                .tracking(-0.3)
                                .foregroundStyle(UNESColor.ink)
                                .lineLimit(1)
                            if let hint = venue.hint {
                                Text(hint)
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundStyle(UNESColor.ink3)
                                    .lineLimit(1)
                            }
                        }
                        Spacer(minLength: 0)
                        if count > 0 {
                            Text("\(count)")
                                .font(.system(size: 12, weight: .bold))
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink3)
                                .padding(EdgeInsets(top: 4, leading: 10, bottom: 4, trailing: 10))
                                .background(UNESColor.surface2, in: Capsule())
                        }
                    }
                    .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
                    .overlay(alignment: .bottom) {
                        if index < store.venues.count - 1 {
                            Rectangle()
                                .fill(UNESColor.line)
                                .frame(height: 0.5)
                                .padding(.leading, 67)
                        }
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .campusEventCard()
    }
}

#Preview("Locais") {
    NavigationStack {
        CampusEventVenuesView(
            store: Store(initialState: CampusEventVenuesFeature.State(event: .preview())) {
                CampusEventVenuesFeature()
            }
        )
    }
}
