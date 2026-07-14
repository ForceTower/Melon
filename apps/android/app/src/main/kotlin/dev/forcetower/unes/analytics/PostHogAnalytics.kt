package dev.forcetower.unes.analytics

import com.posthog.PostHog
import dev.forcetower.melon.core.analytics.Analytics

// PostHog-backed Analytics. The SDK is initialized once in MelonApp; this only
// forwards to the global client. Capture is fire-and-forget (the SDK queues and
// batches off the caller), so it's safe to call on the UI thread.
class PostHogAnalytics : Analytics {
    override fun screen(name: String, properties: Map<String, Any>) {
        PostHog.screen(screenTitle = name, properties = properties)
    }

    override fun selectContent(contentType: String, itemId: String?, properties: Map<String, Any>) {
        val base = buildMap<String, Any> {
            put("content_type", contentType)
            itemId?.let { put("item_id", it) }
            putAll(properties)
        }
        PostHog.capture(event = "select_content", properties = base)
    }

    override fun register(properties: Map<String, Any>) {
        properties.forEach { (key, value) -> PostHog.register(key, value) }
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        PostHog.identify(distinctId = userId, userProperties = properties)
    }

    override fun reset() {
        PostHog.reset()
    }
}
