package dev.forcetower.unes.ui.feature.connected

import androidx.annotation.StringRes
import dev.forcetower.unes.R

// Mirrors iOS `ConnectedTab` (`apps/ios/UNES/Features/Connected/ConnectedView.swift`).
// The enum order is the visual order in the bottom bar.
internal enum class ConnectedTab(@StringRes val labelRes: Int) {
    Overview(R.string.connected_tab_overview),
    Schedule(R.string.connected_tab_schedule),
    Classes(R.string.connected_tab_classes),
    Messages(R.string.connected_tab_messages),
    Me(R.string.connected_tab_me),
}
