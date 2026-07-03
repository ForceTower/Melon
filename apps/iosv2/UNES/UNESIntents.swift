import AppIntents
import SwiftUI
import UNESKit

/// Siri / Shortcuts entry points — thin shells over UNESKit's
/// `IntentSupport`. They live in the app target because the system cannot
/// localize App Intents display strings defined in SPM packages (titles and
/// case names render as raw keys in the Shortcuts app); every key below
/// resolves from this target's `Localizable.xcstrings`.

struct NextClassIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.nextClass.title"

    init() {}

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let answer = IntentSupport.nextClass()
        if let card = answer.card {
            return .result(dialog: answer.dialog, view: card)
        }
        return .result(dialog: answer.dialog, view: EmptyView())
    }
}

struct TodayScheduleIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.today.title"

    init() {}

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let answer = IntentSupport.today()
        if let list = answer.list {
            return .result(dialog: answer.dialog, view: list)
        }
        return .result(dialog: answer.dialog, view: EmptyView())
    }
}

/// The tab bar, as a Siri-visible enum. Display names reuse the tab-title
/// keys, mirrored into this target's catalog; bare literal keys because the
/// metadata extractor can't evaluate anything else.
enum TabDestination: String, AppEnum {
    case home, schedule, classes, messages, me

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "intent.tab.typeName")

    static let caseDisplayRepresentations: [TabDestination: DisplayRepresentation] = [
        .home: DisplayRepresentation(title: "nav.today"),
        .schedule: DisplayRepresentation(title: "nav.schedule"),
        .classes: DisplayRepresentation(title: "nav.classes"),
        .messages: DisplayRepresentation(title: "nav.messages"),
        .me: DisplayRepresentation(title: "nav.me"),
    ]

    var intentTab: IntentTab {
        switch self {
        case .home: .home
        case .schedule: .schedule
        case .classes: .classes
        case .messages: .messages
        case .me: .me
        }
    }
}

struct OpenTabIntent: AppIntent {
    static let title: LocalizedStringResource = "intent.openTab.title"
    static let openAppWhenRun = true

    @Parameter(title: "intent.tab.typeName")
    var tab: TabDestination

    init() {}

    func perform() async throws -> some IntentResult {
        IntentSupport.openTab(tab.intentTab)
        return .result()
    }
}
