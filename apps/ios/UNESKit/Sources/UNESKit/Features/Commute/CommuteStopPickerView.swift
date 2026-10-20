import ComposableArchitecture
import MapKit
import SwiftUI

struct CommuteStopPickerView: View {
    @Bindable var store: StoreOf<CommuteStopPickerFeature>
    @State private var camera: MapCameraPosition = .automatic
    @State private var showsAll = false
    @Environment(\.openURL) private var openURL

    private static let collapsedCount = 8

    var body: some View {
        CommuteSetupScaffold(
            step: 2,
            total: store.draft.totalSteps,
            title: .commuteSetupStopTitle,
            subtitle: .commuteSetupStopSubtitle
        ) {
            SearchField(placeholder: .commuteSetupStopSearch, query: store.query) {
                store.send(.queryChanged($0))
            }
            locationRow
                .padding(.top, 10)
            if store.network != nil {
                map
                    .padding(.top, 14)
                list
                    .padding(.top, 16)
            } else if store.loadFailed {
                UNESBanner(
                    tone: .neutral,
                    title: .localized(.commuteHeroUnavailable),
                    action: .localized(.commonTryAgain),
                    onAction: { store.send(.task) },
                    symbol: "icloud.slash"
                ) {
                    Text(.commuteSetupStopLoadFailed)
                }
                .padding(.top, 16)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 40)
            }
        } footer: {
            Button {
                store.send(.continueTapped)
            } label: {
                Text(store.selectedStop == nil ? .commuteSetupStopPick : .commonContinue)
            }
            .buttonStyle(.unesDark)
            .disabled(store.selectedStop == nil)
        }
        .navigationTitle(Text(.commuteDirectionToCampus))
        .onChange(of: store.userLocation) { _, location in
            guard let location else { return }
            camera = .region(MKCoordinateRegion(center: location.coordinate, latitudinalMeters: 2_400, longitudinalMeters: 2_400))
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Location

    @ViewBuilder
    private var locationRow: some View {
        switch store.location {
        case .denied:
            UNESBanner(
                tone: .neutral,
                title: .localized(.commuteLocationDeniedTitle),
                action: settingsURL == nil ? nil : .localized(.commuteLocationDeniedAction),
                onAction: { settingsURL.map { openURL($0) } },
                symbol: "location.slash"
            ) {
                Text(.commuteLocationDeniedBody)
            }
        case .idle, .locating, .unavailable:
            Button {
                store.send(.useLocationTapped)
            } label: {
                HStack(spacing: 8) {
                    if store.location == .locating {
                        ProgressView()
                    } else {
                        Image(systemName: store.userLocation == nil ? "location" : "location.fill")
                    }
                    Text(locationLabel)
                }
                .font(.system(size: 15, weight: .semibold))
            }
            .buttonStyle(.unesNeutral)
            .disabled(store.location == .locating)
        }
    }

    private var locationLabel: LocalizedStringResource {
        if store.location == .unavailable { return .commuteSetupLocationUnavailable }
        return store.userLocation == nil ? .commuteSetupUseLocation : .commuteSetupNearYou
    }

    private var settingsURL: URL? {
        #if os(iOS)
        URL(string: UIApplication.openSettingsURLString)
        #else
        nil
        #endif
    }

    // MARK: Map

    private var map: some View {
        VStack(alignment: .leading, spacing: 7) {
            Map(
                position: $camera,
                interactionModes: [.pan, .zoom],
                selection: Binding(get: { store.selectedStopId }, set: { store.send(.stopSelected($0)) })
            ) {
                ForEach(store.stops) { stop in
                    Marker(stop.name, systemImage: "bus.fill", coordinate: stop.location.coordinate)
                        .tint(stop.id == store.selectedStopId ? UNESColor.ink : UNESColor.ink3)
                        .tag(stop.id)
                }
                if let location = store.userLocation {
                    Annotation(String.localized(.commuteSetupYou), coordinate: location.coordinate, anchor: .center) {
                        Circle()
                            .fill(.blue)
                            .frame(width: 12, height: 12)
                            .overlay { Circle().strokeBorder(.white, lineWidth: 2.5) }
                            .background { Circle().fill(.blue.opacity(0.18)).frame(width: 26, height: 26) }
                    }
                    .annotationTitles(.hidden)
                }
            }
            .mapStyle(.standard(pointsOfInterest: .excludingAll))
            .frame(height: 188)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous).strokeBorder(UNESColor.cardLine)
            }
            Text(.commuteSetupMapHint)
                .font(.system(size: 10.5, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .padding(.horizontal, 4)
        }
    }

    // MARK: List

    private var list: some View {
        let stops = store.stops
        let visible = showsAll || !store.query.isEmpty ? stops : Array(stops.prefix(Self.collapsedCount))
        return VStack(alignment: .leading, spacing: 0) {
            CommuteSectionHeader(title: .localized(store.userLocation == nil ? .commuteSetupStops : .commuteSetupNearbyStops))
            if stops.isEmpty {
                Text(.commuteSetupNoMatch)
                    .font(.system(size: 13.5))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.horizontal, 4)
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(visible) { stop in
                        row(stop)
                        if stop.id != visible.last?.id {
                            Divider().overlay(UNESColor.line)
                        }
                    }
                }
                .transitCard()
                if visible.count < stops.count {
                    Button {
                        showsAll = true
                    } label: {
                        Text(.commuteSetupShowAllStops(stops.count))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(UNESColor.accent)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func row(_ stop: TransitStop) -> some View {
        let isOn = stop.id == store.selectedStopId
        let lines = store.network?.lines(serving: stop.id, direction: .toCampus) ?? []
        return Button {
            store.send(.stopSelected(stop.id), animation: UNESMotion.ease(0.25))
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "mappin")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(isOn ? UNESColor.ink : UNESColor.ink4)
                VStack(alignment: .leading, spacing: 5) {
                    Text(verbatim: stop.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.15)
                        .foregroundStyle(UNESColor.ink)
                        .multilineTextAlignment(.leading)
                    HStack(spacing: 6) {
                        if let area = stop.area {
                            Text(verbatim: area)
                                .foregroundStyle(UNESColor.ink3)
                        }
                        if let location = store.userLocation {
                            Text(verbatim: "· \(Self.distance(stop.location.distance(to: location)))")
                                .monospacedDigit()
                                .foregroundStyle(UNESColor.ink4)
                        }
                        HStack(spacing: 4) {
                            ForEach(lines, id: \.self) { TransitLineBadge(number: $0, size: .small) }
                        }
                        .padding(.leading, 2)
                    }
                    .font(.system(size: 12))
                    .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                CommuteCheckmark(isOn: isOn)
            }
            .padding(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 14))
            .background(isOn ? UNESColor.surface2 : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    private static func distance(_ meters: Double) -> String {
        Measurement(value: meters, unit: UnitLength.meters)
            .formatted(.measurement(width: .abbreviated, usage: .road, numberFormatStyle: .number.precision(.fractionLength(0...1))))
    }
}

#Preview {
    NavigationStack {
        CommuteStopPickerView(store: Store(initialState: CommuteStopPickerFeature.State(
            draft: CommuteSetupDraft(directions: [.toCampus, .fromCampus], existing: CommutePlan())
        )) {
            CommuteStopPickerFeature()
        } withDependencies: {
            $0.transitRepository = .mock(mode: .normal)
        })
    }
}
