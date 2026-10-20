import ComposableArchitecture
import MapKit
import SwiftUI

struct CommuteFollowView: View {
    @Bindable var store: StoreOf<CommuteFollowFeature>
    @State private var camera: MapCameraPosition = .automatic
    @Environment(\.openURL) private var openURL

    private var color: Color { UNESColor.transitLine(store.lineNumber) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    VStack(alignment: .leading, spacing: 0) {
                        if store.loadFailed || store.state.isOutdated(at: context.date) {
                            UNESBanner(tone: .neutral, title: .localized(.commuteOfflineTitle), symbol: "wifi.slash") {
                                Text(.commuteOfflineBody(CommuteFormat.clock(store.followReceivedAt)))
                            }
                            .padding(.bottom, 14)
                        }
                        headline(now: context.date)
                        status(now: context.date)
                    }
                }
                map
                mapCaption
                if let route = store.route, let mine = route.index(of: store.stopId) {
                    progress(route: route, mineIndex: mine)
                        .padding(.top, 14)
                }
                locationButton
                    .padding(.top, 12)
                if store.locationDenied {
                    locationDeniedNotice
                        .padding(.top, 10)
                }
            }
            .padding(EdgeInsets(top: 4, leading: 16, bottom: 24, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .background(UNESColor.surface.ignoresSafeArea())
        .navigationTitle(Text(.commuteFollowTitle(store.lineNumber)))
        .inlineNavigationBar()
        .toolbar {
            ToolbarItem(placement: .trailingCompat) {
                Button {
                    store.send(.explainTapped)
                } label: {
                    Image(systemName: "info.circle")
                }
                .accessibilityLabel(Text(.commuteHowTitle))
            }
        }
        .sheet(isPresented: Binding(
            get: { store.isExplainerPresented },
            set: { if !$0 { store.send(.explainerDismissed) } }
        )) {
            CommuteHowSheet(arrival: store.arrival, direction: store.direction)
        }
        .onChange(of: store.route?.id) { _, _ in
            if let region = store.route.flatMap(Self.region(for:)) {
                camera = .region(region)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Headline

    @ViewBuilder
    private func headline(now: Date) -> some View {
        let frozen = store.state.isOutdated(at: now)
        VStack(alignment: .leading, spacing: 6) {
            if let arrival = store.arrival {
                HStack(alignment: .firstTextBaseline, spacing: 12) {
                    TransitEstimateText(
                        estimate: CommuteFormat.estimate(for: arrival, now: now, frozen: frozen),
                        dim: arrival.state == .historical || frozen,
                        stale: arrival.signalLost || frozen,
                        size: 46
                    )
                    Text(isDepartureStop ? .commuteFollowUntilDeparture : .commuteFollowUntilYourStop)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
                if let detail = detail(arrival) {
                    Text(verbatim: detail)
                        .font(.system(size: 15, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                }
            } else if store.follow != nil {
                Text(hasPassedStop ? .commuteFollowPassed : .commuteFollowNoEstimate)
                    .font(.system(size: 22, weight: .bold))
                    .tracking(-0.66)
                    .foregroundStyle(UNESColor.ink)
            } else if store.loadFailed {
                Text(.commuteHeroUnavailable)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
            }
        }
        .opacity(store.arrival?.signalLost == true ? 0.55 : 1)
        .padding(EdgeInsets(top: 4, leading: 4, bottom: 14, trailing: 4))
    }

    private var isDepartureStop: Bool {
        store.direction == .fromCampus && store.route?.stops.first?.id == store.stopId
    }

    /// Only a bus running this route beyond the stop has passed it; one still
    /// finishing the opposite trip simply has no estimate yet.
    private var hasPassedStop: Bool {
        guard let progress = store.followedVehicle?.progressMeters,
              let stop = store.route?.stops.first(where: { $0.id == store.stopId })
        else { return false }
        return progress > stop.distanceMeters
    }

    private func detail(_ arrival: TransitArrival) -> String? {
        var parts: [String] = []
        // The count includes the student's stop, so 1 means it is the next one.
        if let stops = arrival.stopsAway, stops > 0, arrival.state == .live {
            parts.append(stops == 1 ? .localized(.commuteFollowNextStop) : .localized(.commuteFollowStopsAway(stops)))
        }
        if let campus = arrival.campusArrival {
            parts.append(.localized(.commuteReachesCampus(CommuteFormat.clock(campus))))
        } else if arrival.state == .waiting, let departure = arrival.originDeparture {
            parts.append(.localized(.commuteUsuallyLeaves(CommuteFormat.clock(departure))))
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    @ViewBuilder
    private func status(now: Date) -> some View {
        if let arrival = store.arrival {
            HStack(spacing: 8) {
                if store.state.isOutdated(at: now) {
                    TransitChip(kind: .stale, text: .localized(.commuteStateOffline))
                } else {
                    TransitChip(kind: TransitChip.Kind(arrival))
                    if let fix = arrival.lastFixAt, arrival.state != .historical {
                        TransitFreshness(fixAt: fix, now: now, signalLost: arrival.signalLost)
                    }
                }
            }
            .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
        }
    }

    // MARK: Map

    private var map: some View {
        Map(position: $camera, interactionModes: [.pan, .zoom]) {
            if let route = store.route {
                MapPolyline(coordinates: route.path.map(\.coordinate))
                    .stroke(color, style: StrokeStyle(lineWidth: 5, lineCap: .round, lineJoin: .round))
                ForEach(route.stops) { stop in
                    stopAnnotation(stop, in: route)
                }
            }
            ForEach(store.routeVehicles) { vehicle in
                Annotation(
                    String.localized(.commuteLineAccessibility(vehicle.lineNumber)),
                    coordinate: vehicle.location.coordinate,
                    anchor: .center
                ) {
                    busMarker(vehicle)
                }
                .annotationTitles(.hidden)
            }
            if store.showsMyLocation {
                UserAnnotation()
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        .frame(height: 332)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous).strokeBorder(UNESColor.cardLine)
        }
    }

    @MapContentBuilder
    private func stopAnnotation(_ stop: TransitStop, in route: TransitRoute) -> some MapContent {
        if stop.id == store.stopId {
            Annotation(String.localized(.commuteRouteYourStop), coordinate: stop.location.coordinate, anchor: .center) {
                Circle()
                    .fill(UNESColor.card)
                    .frame(width: 13, height: 13)
                    .overlay { Circle().strokeBorder(UNESColor.ink, lineWidth: 2.5) }
                    .overlay { Circle().fill(UNESColor.ink).frame(width: 4.5, height: 4.5) }
                    .background { Circle().fill(color.opacity(0.2)).frame(width: 24, height: 24) }
            }
        } else if stop.id == route.campusStopId {
            Annotation("UEFS", coordinate: stop.location.coordinate, anchor: .center) {
                Image(systemName: "graduationcap.fill")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .frame(width: 26, height: 26)
                    .background(UNESColor.card, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .overlay { RoundedRectangle(cornerRadius: 8, style: .continuous).strokeBorder(UNESColor.line) }
            }
        } else {
            Annotation(stop.name, coordinate: stop.location.coordinate, anchor: .center) {
                Circle()
                    .fill(UNESColor.card)
                    .frame(width: 7, height: 7)
                    .overlay { Circle().strokeBorder(color, lineWidth: 2) }
            }
            .annotationTitles(.hidden)
        }
    }

    private func busMarker(_ vehicle: TransitVehicle) -> some View {
        let followed = vehicle.id == store.vehicleId || store.vehicleId == nil
        return Image(systemName: vehicle.heading == nil ? "circle.fill" : "location.north.fill")
            .font(.system(size: vehicle.heading == nil ? 12 : 17, weight: .bold))
            .foregroundStyle(color)
            .rotationEffect(.degrees(vehicle.heading ?? 0))
            .shadow(color: .black.opacity(0.3), radius: 2, y: 1)
            .padding(4)
            .background(UNESColor.card.opacity(0.9), in: Circle())
            .opacity(followed ? 1 : 0.5)
            .scaleEffect(followed ? 1 : 0.85)
            .animation(UNESMotion.settle(0.8), value: vehicle.location)
    }

    private var mapCaption: some View {
        Text(.commuteFollowCaption(store.lineNumber, store.routeVehicles.count))
            .font(.system(size: 10.5, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink4)
            .padding(EdgeInsets(top: 7, leading: 4, bottom: 0, trailing: 4))
    }

    // MARK: Route line

    private func progress(route: TransitRoute, mineIndex: Int) -> some View {
        let end = route.direction == .toCampus
            ? (route.campusStopId.flatMap(route.index(of:)) ?? route.stops.count - 1)
            : route.stops.count - 1
        let stops = Array(route.stops[...max(end, mineIndex)])
        return TransitRouteProgress(
            stopNames: stops.map { transitShortName($0, in: route) },
            busPosition: store.followedVehicle?.progressMeters.flatMap { Self.fractionalIndex($0, stops: stops) },
            mineIndex: mineIndex,
            color: color
        )
        .transitCard(padding: EdgeInsets(top: 14, leading: 14, bottom: 10, trailing: 14))
    }

    /// Where `meters` falls between the stops, as a fractional stop index.
    static func fractionalIndex(_ meters: Double, stops: [TransitStop]) -> Double? {
        guard let first = stops.first, let last = stops.last, meters >= first.distanceMeters else { return nil }
        guard meters < last.distanceMeters else { return Double(stops.count - 1) }
        for index in stops.indices.dropLast() {
            let from = stops[index].distanceMeters, to = stops[index + 1].distanceMeters
            if meters < to {
                return Double(index) + (to > from ? (meters - from) / (to - from) : 0)
            }
        }
        return Double(stops.count - 1)
    }

    // MARK: Location

    private var locationButton: some View {
        Button {
            store.send(.myLocationTapped)
        } label: {
            Label {
                Text(store.showsMyLocation ? .commuteHideMyLocation : .commuteShowMyLocation)
            } icon: {
                Image(systemName: "location")
            }
            .font(.system(size: 15, weight: .semibold))
        }
        .buttonStyle(.unesNeutral)
    }

    private var locationDeniedNotice: some View {
        UNESBanner(
            tone: .neutral,
            title: .localized(.commuteLocationDeniedTitle),
            action: settingsURL == nil ? nil : .localized(.commuteLocationDeniedAction),
            onAction: { settingsURL.map { openURL($0) } },
            symbol: "location.slash"
        ) {
            Text(.commuteLocationDeniedFollowBody)
        }
    }

    private var settingsURL: URL? {
        #if os(iOS)
        URL(string: UIApplication.openSettingsURLString)
        #else
        nil
        #endif
    }

    static func region(for route: TransitRoute) -> MKCoordinateRegion? {
        let points = route.path.isEmpty ? route.stops.map(\.location) : route.path
        guard let first = points.first else { return nil }
        var minLat = first.latitude, maxLat = first.latitude
        var minLon = first.longitude, maxLon = first.longitude
        for point in points {
            minLat = min(minLat, point.latitude)
            maxLat = max(maxLat, point.latitude)
            minLon = min(minLon, point.longitude)
            maxLon = max(maxLon, point.longitude)
        }
        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: (minLat + maxLat) / 2, longitude: (minLon + maxLon) / 2),
            span: MKCoordinateSpan(latitudeDelta: (maxLat - minLat) * 1.25 + 0.002, longitudeDelta: (maxLon - minLon) * 1.25 + 0.002)
        )
    }
}

extension GeoPoint {
    var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

#Preview {
    NavigationStack {
        CommuteFollowView(store: Store(initialState: CommuteFollowFeature.State(
            arrival: TransitFixtures.board(now: .now).arrivals[0],
            direction: .toCampus,
            stopId: TransitFixtures.myStopId,
            receivedAt: .now
        )) {
            CommuteFollowFeature()
        } withDependencies: {
            $0.transitRepository = .mock(mode: .normal)
        })
    }
}
