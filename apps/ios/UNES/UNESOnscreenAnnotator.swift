import AppIntents
import UNESKit

/// Pairs what a UNESKit screen shows with the entity type that represents
/// it, so Siri can resolve "this" to the discipline or message on screen.
/// The types live here; the identifier format lives in the package.
enum UNESOnscreenAnnotator {
    static let identifier: OnscreenEntityAnnotator = { entity in
        switch entity {
        case .discipline:
            EntityIdentifier(for: DisciplineEntity.self, identifier: entity.spotlightIdentifier)
        case .message:
            EntityIdentifier(for: MessageEntity.self, identifier: entity.spotlightIdentifier)
        }
    }
}
