package dev.forcetower.melon.feature.settings.domain.usecase

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.settings.domain.model.UserSettings
import dev.forcetower.melon.feature.settings.domain.model.UserSettingsPatch
import dev.forcetower.melon.feature.settings.domain.repository.SettingsError
import dev.forcetower.melon.feature.settings.domain.repository.SettingsRepository
import dev.zacsweers.metro.Inject

// Default-null kwargs match the convention from
// `RegisterNotificationTokenUseCase` so iOS callers can flip a single toggle
// without naming all ten fields. Pass null/omit any field to leave it
// unchanged.
@Inject
class UpdateSettingsUseCase internal constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(
        gradeSpoiler: Int? = null,
        notifMsgBroadcast: Boolean? = null,
        notifMsgClass: Boolean? = null,
        notifMsgDirect: Boolean? = null,
        notifGradePosted: Boolean? = null,
        notifGradeChanged: Boolean? = null,
        notifGradeDateChanged: Boolean? = null,
        notifClassLocation: Boolean? = null,
        notifClassMaterial: Boolean? = null,
        notifClassSubject: Boolean? = null,
    ): Outcome<UserSettings, SettingsError> =
        repository.update(
            UserSettingsPatch(
                gradeSpoiler = gradeSpoiler,
                notifMsgBroadcast = notifMsgBroadcast,
                notifMsgClass = notifMsgClass,
                notifMsgDirect = notifMsgDirect,
                notifGradePosted = notifGradePosted,
                notifGradeChanged = notifGradeChanged,
                notifGradeDateChanged = notifGradeDateChanged,
                notifClassLocation = notifClassLocation,
                notifClassMaterial = notifClassMaterial,
                notifClassSubject = notifClassSubject,
            ),
        )
}
