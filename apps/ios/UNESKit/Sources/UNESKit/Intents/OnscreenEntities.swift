import AppIntents
import ComposableArchitecture
import SwiftUI

/// What a screen is showing, in the currency the app target can turn into
/// an `EntityIdentifier` — the entity types live there, next to the
/// catalog the metadata extractor reads.
public enum OnscreenEntity: Equatable, Sendable {
    case discipline(semesterId: String, disciplineId: String)
    case message(id: String)

    /// The Spotlight identifier of the same entity — what the app target
    /// pairs with the entity type.
    public var spotlightIdentifier: String {
        switch self {
        case let .discipline(semesterId, disciplineId):
            SpotlightEntityID.discipline(semesterId: semesterId, disciplineId: disciplineId)
        case let .message(id):
            SpotlightEntityID.message(id: id)
        }
    }
}

/// Installed by the app target at launch; nil when nothing is installed
/// (previews, tests), in which case screens annotate nothing.
public typealias OnscreenEntityAnnotator = @Sendable (OnscreenEntity) -> EntityIdentifier?

private enum OnscreenEntityAnnotatorKey: DependencyKey {
    static let liveValue: OnscreenEntityAnnotator? = nil
    static let testValue: OnscreenEntityAnnotator? = nil
}

extension DependencyValues {
    var onscreenEntityAnnotator: OnscreenEntityAnnotator? {
        get { self[OnscreenEntityAnnotatorKey.self] }
        set { self[OnscreenEntityAnnotatorKey.self] = newValue }
    }
}

extension SpotlightSupport {
    /// Called once from `AppDelegate.didFinishLaunching`, next to the
    /// indexer.
    public static func installAnnotator(_ annotator: @escaping OnscreenEntityAnnotator) {
        prepareDependencies {
            $0.onscreenEntityAnnotator = annotator
        }
    }
}

extension View {
    /// Tells Siri which entity this view shows, so "this" resolves to it.
    /// A no-op below iOS 18.4 and when no annotator is installed.
    func onscreenEntity(_ entity: OnscreenEntity?) -> some View {
        modifier(OnscreenEntityModifier(entity: entity))
    }
}

private struct OnscreenEntityModifier: ViewModifier {
    @Dependency(\.onscreenEntityAnnotator) private var annotator
    var entity: OnscreenEntity?

    func body(content: Content) -> some View {
        if #available(iOS 18.4, macOS 15.4, watchOS 11.4, *), let entity, let identifier = annotator?(entity) {
            content.appEntityIdentifier(identifier)
        } else {
            content
        }
    }
}
