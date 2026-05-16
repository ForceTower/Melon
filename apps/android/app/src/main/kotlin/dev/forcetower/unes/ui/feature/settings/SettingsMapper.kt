package dev.forcetower.unes.ui.feature.settings

import dev.forcetower.melon.feature.settings.domain.model.UserSettings as KmpUserSettings

// KMP `UserSettings` snapshot → presentation state. Mirrors iOS
// `SettingsViewModel.applySnapshot`: every field is overwritten with the
// canonical server value (no-op for booleans, defensive for the spoiler
// integer in case the server ever clamps it to a different range).
internal fun SettingsUiState.applySnapshot(snapshot: KmpUserSettings): SettingsUiState =
    copy(
        spoiler = SpoilerMode.fromServerInt(snapshot.gradeSpoiler) ?: spoiler,
        notifMsgBroadcast = snapshot.notifMsgBroadcast,
        notifMsgClass = snapshot.notifMsgClass,
        notifMsgDirect = snapshot.notifMsgDirect,
        notifGradePosted = snapshot.notifGradePosted,
        notifGradeChanged = snapshot.notifGradeChanged,
        notifGradeDateChanged = snapshot.notifGradeDateChanged,
        notifClassLocation = snapshot.notifClassLocation,
        notifClassMaterial = snapshot.notifClassMaterial,
        notifClassSubject = snapshot.notifClassSubject,
    )
