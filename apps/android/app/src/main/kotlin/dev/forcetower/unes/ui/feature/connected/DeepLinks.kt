package dev.forcetower.unes.ui.feature.connected

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

// `unes://` deeplinks — carried by push notifications (the backend puts the
// URL in the FCM `data.url` key) and by plain VIEW intents. A URL resolves to
// one of the typed targets below; `ConnectedScreen` applies it by rewriting
// the matching tab's back stack. Nav3 has no URI routing of its own, so this
// parser is the whole mapping surface.
internal sealed interface DeepLinkTarget {
    data class Tab(val tab: ConnectedTab) : DeepLinkTarget
    data class Message(val id: String) : DeepLinkTarget
    data class MaterialsDiscipline(val disciplineId: String) : DeepLinkTarget
    data class MaterialDetail(val materialId: String) : DeepLinkTarget
}

// Hand-rolled instead of `android.net.Uri` so it stays a pure JVM function
// (plain-JUnit testable). Unknown or malformed URLs resolve to null and the
// link is dropped.
internal fun parseDeepLink(url: String): DeepLinkTarget? {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd < 0 || !url.take(schemeEnd).equals("unes", ignoreCase = true)) return null
    val afterScheme = url.substring(schemeEnd + 3).substringBefore('#').substringBefore('?')
    val segments = afterScheme.split('/').filter { it.isNotEmpty() }

    val host = segments.firstOrNull()?.lowercase() ?: return null
    val rest = segments.drop(1)
    return when {
        rest.isEmpty() -> tabFor(host)?.let(DeepLinkTarget::Tab)
        host == "messages" && rest.size == 1 -> DeepLinkTarget.Message(rest[0])
        host == "materials" && rest.size == 2 && rest[0].equals("discipline", ignoreCase = true) ->
            DeepLinkTarget.MaterialsDiscipline(rest[1])
        host == "materials" && rest.size == 1 -> DeepLinkTarget.MaterialDetail(rest[0])
        else -> null
    }
}

private fun tabFor(host: String): ConnectedTab? = when (host) {
    "home" -> ConnectedTab.Overview
    "schedule" -> ConnectedTab.Schedule
    "classes" -> ConnectedTab.Classes
    "messages" -> ConnectedTab.Messages
    "me" -> ConnectedTab.Me
    else -> null
}

// Rewrites the target tab's stack to the synthesized path a user would have
// walked organically, so back behaves as if they had. Reset-to-root on
// purpose: a notification tap should land in a predictable place even when
// the tab already had depth.
internal fun ConnectedNavigator.open(target: DeepLinkTarget) {
    when (target) {
        is DeepLinkTarget.Tab -> selectTab(target.tab)
        is DeepLinkTarget.Message -> setStack(
            ConnectedTab.Messages,
            listOf(ConnectedRoute.MessagesList, ConnectedRoute.MessageDetail(target.id)),
        )
        is DeepLinkTarget.MaterialsDiscipline -> setStack(
            ConnectedTab.Me,
            listOf(
                ConnectedRoute.Me,
                ConnectedRoute.Materials,
                ConnectedRoute.MaterialsDiscipline(target.disciplineId),
            ),
        )
        is DeepLinkTarget.MaterialDetail -> setStack(
            ConnectedTab.Me,
            listOf(
                ConnectedRoute.Me,
                ConnectedRoute.Materials,
                ConnectedRoute.MaterialsDetail(target.materialId),
            ),
        )
    }
}

// Buffers the tap between `MainActivity` (where the intent lands) and
// `ConnectedScreen` (where the navigator lives). Conflated on purpose: the
// shell may not exist yet — cold start, or the user is mid-onboarding — so
// the target waits here until the authenticated shell composes and collects,
// and a newer tap replaces a stale undelivered one.
@Singleton
internal class DeepLinkHandler @Inject constructor() {
    private val pending = Channel<DeepLinkTarget>(Channel.CONFLATED)

    val targets: Flow<DeepLinkTarget> = pending.receiveAsFlow()

    fun offer(url: String) {
        parseDeepLink(url)?.let { pending.trySend(it) }
    }
}
