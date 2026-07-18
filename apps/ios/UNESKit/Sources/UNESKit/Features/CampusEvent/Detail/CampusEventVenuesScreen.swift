import ComposableArchitecture
import SwiftUI

/// Where the event happens: an illustrated campus map (when the venues carry
/// coordinates) over the venue list. Pin selection is view-only state.
@Reducer
struct CampusEventVenuesFeature {
    @ObservableState
    struct State: Equatable {
        let event: CampusEvent

        var venues: [CampusEventVenue] { event.venues }
    }

    enum Action: Equatable {
        case task
    }

    @Dependency(\.analytics) var analytics

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .task:
                analytics.screen(Screens.campusEventVenues)
                return .none
            }
        }
    }
}

private let campusMapAspectRatio: CGFloat = 1368 / 1150
private let campusMapMaxZoom: CGFloat = 3
private let campusMapDoubleTapZoom: CGFloat = 2

struct CampusEventVenuesView: View {
    let store: StoreOf<CampusEventVenuesFeature>

    @State private var selectedId: CampusEventVenue.ID?
    @State private var mapZoom: CGFloat = 1
    @State private var mapPan: CGPoint = .zero
    @State private var pinchStart: (zoom: CGFloat, pan: CGPoint)?
    @State private var dragStartPan: CGPoint?

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
        .task { await store.send(.task).finish() }
    }

    private var mappedVenues: [CampusEventVenue] {
        store.venues.filter { $0.mapX != nil && $0.mapY != nil }
    }

    private func tone(for venue: CampusEventVenue) -> Color {
        let index = store.venues.firstIndex { $0.id == venue.id } ?? 0
        return campusEventPalette[index % campusEventPalette.count]
    }

    // MARK: Campus map

    private var map: some View {
        GeometryReader { proxy in
            let size = proxy.size
            ZStack(alignment: .topLeading) {
                Image("CampusMap", bundle: .module)
                    .resizable()
                    .frame(width: size.width, height: size.height)
                    .scaleEffect(mapZoom, anchor: .topLeading)
                    .offset(x: mapPan.x, y: mapPan.y)
                    .allowsHitTesting(false)

                // Pins follow the map transform but keep their own size.
                ForEach(mappedVenues) { venue in
                    pin(for: venue)
                        .position(
                            x: size.width * (venue.mapX ?? 0) / 100 * mapZoom + mapPan.x,
                            y: size.height * (venue.mapY ?? 0) / 100 * mapZoom + mapPan.y
                        )
                }
            }
            .contentShape(Rectangle())
            .gesture(doubleTapGesture(in: size))
            #if !os(watchOS)
            .gesture(pinchGesture(in: size))
            #endif
            // Only claims drags from the scroll view while zoomed in.
            .highPriorityGesture(panGesture(in: size), isEnabled: mapZoom > 1)
        }
        // Keeps the card at the image's aspect ratio so the venues'
        // percentage pin coordinates land on the right map features.
        .aspectRatio(campusMapAspectRatio, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.08), radius: 12, y: 8)
    }

    #if !os(watchOS)
    private func pinchGesture(in size: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                let start = pinchStart ?? (mapZoom, mapPan)
                pinchStart = start
                let zoom = min(max(start.zoom * value.magnification, 1), campusMapMaxZoom)
                // Keep the map point under the gesture anchor fixed while zooming.
                let anchor = CGPoint(
                    x: value.startAnchor.x * size.width,
                    y: value.startAnchor.y * size.height
                )
                mapZoom = zoom
                mapPan = clampedPan(
                    CGPoint(
                        x: anchor.x - (anchor.x - start.pan.x) * (zoom / start.zoom),
                        y: anchor.y - (anchor.y - start.pan.y) * (zoom / start.zoom)
                    ),
                    zoom: zoom,
                    in: size
                )
            }
            .onEnded { _ in pinchStart = nil }
    }
    #endif

    private func doubleTapGesture(in size: CGSize) -> some Gesture {
        SpatialTapGesture(count: 2)
            .onEnded { value in
                let zoom: CGFloat = mapZoom > 1 ? 1 : campusMapDoubleTapZoom
                let pan = clampedPan(
                    CGPoint(
                        x: value.location.x - (value.location.x - mapPan.x) * (zoom / mapZoom),
                        y: value.location.y - (value.location.y - mapPan.y) * (zoom / mapZoom)
                    ),
                    zoom: zoom,
                    in: size
                )
                withAnimation(UNESMotion.ease()) {
                    mapZoom = zoom
                    mapPan = pan
                }
            }
    }

    private func panGesture(in size: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                let start = dragStartPan ?? mapPan
                dragStartPan = start
                mapPan = clampedPan(
                    CGPoint(
                        x: start.x + value.translation.width,
                        y: start.y + value.translation.height
                    ),
                    zoom: mapZoom,
                    in: size
                )
            }
            .onEnded { _ in dragStartPan = nil }
    }

    /// Keeps the scaled map covering the whole viewport (no gaps at the edges).
    private func clampedPan(_ pan: CGPoint, zoom: CGFloat, in size: CGSize) -> CGPoint {
        CGPoint(
            x: min(max(pan.x, size.width * (1 - zoom)), 0),
            y: min(max(pan.y, size.height * (1 - zoom)), 0)
        )
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
                    // Plain buttons only hit-test opaque pixels; the row has
                    // no background of its own.
                    .contentShape(Rectangle())
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
