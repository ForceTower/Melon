import PostHog
import UNESKit

/// PostHog-backed `AnalyticsSink`. The SDK is initialized once in
/// `AppDelegate.configurePostHog`; this only forwards to the global client.
struct PostHogAnalyticsSink: AnalyticsSink {
    func screen(name: String, properties: [String: any Sendable]) {
        PostHogSDK.shared.screen(name, properties: properties)
    }

    func selectContent(contentType: String, itemId: String?, properties: [String: any Sendable]) {
        var base: [String: any Sendable] = ["content_type": contentType]
        if let itemId { base["item_id"] = itemId }
        base.merge(properties) { _, new in new }
        PostHogSDK.shared.capture("select_content", properties: base)
    }

    func register(properties: [String: any Sendable]) {
        PostHogSDK.shared.register(properties)
    }

    func identify(userId: String, properties: [String: any Sendable]) {
        PostHogSDK.shared.identify(userId, userProperties: properties)
    }

    func reset() {
        PostHogSDK.shared.reset()
    }
}
