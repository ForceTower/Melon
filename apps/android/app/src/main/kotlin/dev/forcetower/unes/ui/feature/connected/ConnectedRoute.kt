package dev.forcetower.unes.ui.feature.connected

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Inner-Connected navigation routes. Each tab has a root route that's the
// stack floor; non-root routes (e.g. `MessageDetail`) are pushed onto
// whichever tab is active when the user opens them. Mirrors the per-tab
// `NavigationStack` pattern in `ConnectedView.swift` on iOS — switching
// tabs preserves each tab's depth, system back pops within a tab.
internal sealed interface ConnectedRoute : NavKey {
    @Serializable data object Overview : ConnectedRoute
    @Serializable data object Schedule : ConnectedRoute
    @Serializable data object Classes : ConnectedRoute
    // Detail screen pushed onto the Classes stack when a discipline card is
    // tapped. `offerId` is the only identifier needed to scope the
    // `ObserveDisciplineDetailUseCase` flow; the seed Discipline is handed
    // off in-memory through `DisciplinesListViewModel.openSeed`.
    @Serializable data class DisciplineDetail(val offerId: String) : ConnectedRoute
    @Serializable data object MessagesList : ConnectedRoute
    @Serializable data class MessageDetail(val id: String) : ConnectedRoute
    @Serializable data object Me : ConnectedRoute
    // Pushed onto the Me stack when the "Configurações" row is tapped — the
    // editorial settings hub (credential vault + spoiler picker + per-row
    // notification toggles). Mirrors iOS `SettingsView`.
    @Serializable data object Settings : ConnectedRoute
    // Pushed onto the active tab when the "Calendário" shortcut is tapped on
    // the Me hub. No payload — the screen drives off the KMP events flow.
    @Serializable data object Calendar : ConnectedRoute
    // Pushed onto the active tab when the "Final Countdown" shortcut is tapped
    // on the Me hub. Fixture-driven calculator — no payload, no KMP wiring.
    @Serializable data object FinalCountdown : ConnectedRoute
}

internal fun ConnectedTab.rootRoute(): ConnectedRoute = when (this) {
    ConnectedTab.Overview -> ConnectedRoute.Overview
    ConnectedTab.Schedule -> ConnectedRoute.Schedule
    ConnectedTab.Classes -> ConnectedRoute.Classes
    ConnectedTab.Messages -> ConnectedRoute.MessagesList
    ConnectedTab.Me -> ConnectedRoute.Me
}
