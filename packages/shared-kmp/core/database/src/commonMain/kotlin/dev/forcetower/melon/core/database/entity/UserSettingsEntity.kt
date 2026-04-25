package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Mirror of the server-side `user_settings` row. Single-row table per
// logged-in user; `gradeSpoiler` is `0=value`, `1=comment`, `2=posted` (the
// SpoilerMode enum on the iOS side). Notification flags map 1:1 to the
// toggles in the Settings screen.
@Entity(tableName = "UserSettings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
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
