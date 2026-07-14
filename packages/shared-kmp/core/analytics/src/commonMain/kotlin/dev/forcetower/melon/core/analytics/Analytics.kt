package dev.forcetower.melon.core.analytics

// Product-analytics sink. Like CrashReporter, the KMP framework only defines
// the interface — the concrete implementation (PostHog on Android, mirrored in
// Swift on iOS) is host-provided through UmbrellaConfig, so call sites depend on
// this, never on a vendor SDK, and tests get a no-op.
//
// Deliberately small (GA4-style): most product questions are answered by "what
// screen did they see" and "what did they tap". Add a method only when
// something genuinely isn't a screen view or a content selection.
interface Analytics {
    // A screen was shown — "screen_view". `name` should come from [Screens].
    fun screen(name: String, properties: Map<String, Any> = emptyMap())

    // A tap/open on a thing (a hub, a material, a ranking entry…) —
    // "select_content". `contentType` should come from [ContentTypes].
    fun selectContent(
        contentType: String,
        itemId: String? = null,
        properties: Map<String, Any> = emptyMap(),
    )

    // Persistent properties stamped on every event, anonymous and identified —
    // e.g. the device machine_id, so PostHog events line up with the OTel logs
    // (both keyed on the same id). These do NOT survive [reset], so re-apply
    // device context after a logout.
    fun register(properties: Map<String, Any>)

    // Tie subsequent events to a person instead of just the device — call on
    // login. Without it, analytics counts devices, not users, and can't stitch
    // a person's phone + watch together.
    fun identify(userId: String, properties: Map<String, Any> = emptyMap())

    // Unlink the device from the person — call on logout.
    fun reset()
}

object NoOpAnalytics : Analytics {
    override fun screen(name: String, properties: Map<String, Any>) = Unit
    override fun selectContent(contentType: String, itemId: String?, properties: Map<String, Any>) = Unit
    override fun register(properties: Map<String, Any>) = Unit
    override fun identify(userId: String, properties: Map<String, Any>) = Unit
    override fun reset() = Unit
}
