package dev.forcetower.melon.feature.settings.domain.model

// Domain shape consumed by `ObserveSettingsUseCase`. Mirrors the Room entity
// minus the userId — the iOS side only ever sees the singleton row, so the
// id is noise at the UI boundary.
data class UserSettings(
    val gradeSpoiler: Int,
    val notifMsgBroadcast: Boolean,
    val notifMsgClass: Boolean,
    val notifMsgDirect: Boolean,
    val notifGradePosted: Boolean,
    val notifGradeChanged: Boolean,
    val notifGradeDateChanged: Boolean,
    val notifClassLocation: Boolean,
    val notifClassMaterial: Boolean,
    val notifClassSubject: Boolean,
)

// All-nullable patch shape. Null = leave alone. The Settings UI hands back
// one of these per toggle; the use case writes to the local DAO and pushes
// the same patch over the wire.
data class UserSettingsPatch(
    val gradeSpoiler: Int? = null,
    val notifMsgBroadcast: Boolean? = null,
    val notifMsgClass: Boolean? = null,
    val notifMsgDirect: Boolean? = null,
    val notifGradePosted: Boolean? = null,
    val notifGradeChanged: Boolean? = null,
    val notifGradeDateChanged: Boolean? = null,
    val notifClassLocation: Boolean? = null,
    val notifClassMaterial: Boolean? = null,
    val notifClassSubject: Boolean? = null,
)
