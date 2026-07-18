import ComposableArchitecture
import Foundation

@Reducer
struct LicensesFeature {
    @ObservableState
    struct State: Equatable {
        var query = ""
        /// The active filter chip; `nil` is "Todos".
        var family: LicenseFamily?
        /// The one open row — tapping another collapses the previous.
        var expandedID: LicensePackage.ID?
        /// Which package's pin just landed on the pasteboard — drives the
        /// transient "copiado" feedback.
        var copiedID: LicensePackage.ID?

        var groups: [LicenseGroup] {
            let matching = matchingPackages
            return LicenseCatalog.breakdown.compactMap { share in
                let packages = matching.filter { $0.family == share.family }
                guard !packages.isEmpty else { return nil }
                return LicenseGroup(family: share.family, packages: packages, familyCount: share.count)
            }
        }

        private var matchingPackages: [LicensePackage] {
            LicenseCatalog.packages.filter { package in
                if let family, package.family != family { return false }
                let query = query.trimmingCharacters(in: .whitespaces)
                guard !query.isEmpty else { return true }
                return package.name.localizedStandardContains(query)
                    || package.author.localizedStandardContains(query)
                    || String.localized(package.category).localizedStandardContains(query)
            }
        }
    }

    enum Action: Equatable {
        case task
        case queryChanged(String)
        case familySelected(LicenseFamily?)
        case packageToggled(LicensePackage.ID)
        case homepageTapped(LicensePackage.ID)
        case licenseTextTapped(LicenseFamily)
        case copyTapped(LicensePackage.ID)
        case copyFeedbackExpired
    }

    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.openURL) var openURL
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    private enum CancelID { case copyFeedback }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.licenses)
                return .none

            case let .queryChanged(query):
                state.query = query
                return .none

            case let .familySelected(family):
                state.family = family
                return .none

            case let .packageToggled(id):
                state.expandedID = state.expandedID == id ? nil : id
                return .none

            case let .homepageTapped(id):
                guard let url = LicenseCatalog.package(id)?.homepageURL else { return .none }
                return .run { _ in await openURL(url) }

            case let .licenseTextTapped(family):
                return .run { _ in await openURL(family.textURL) }

            case let .copyTapped(id):
                guard let package = LicenseCatalog.package(id) else { return .none }
                state.copiedID = id
                return .run { send in
                    await pasteboard.copy(package.pin)
                    try await clock.sleep(for: .milliseconds(1400))
                    await send(.copyFeedbackExpired)
                }
                .cancellable(id: CancelID.copyFeedback, cancelInFlight: true)

            case .copyFeedbackExpired:
                state.copiedID = nil
                return .none
            }
        }
    }
}

/// One license-family card: the packages that survived the filter, plus the
/// family's full size for the "N de M" pill.
struct LicenseGroup: Equatable, Identifiable {
    let family: LicenseFamily
    let packages: [LicensePackage]
    let familyCount: Int

    var id: LicenseFamily { family }
}
