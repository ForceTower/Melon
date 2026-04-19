package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// K/V store for user-editable preferences (grade_spoiler, theme, etc.). Decoupled
// from `SyncState` so wiping prefs doesn't nuke the 1h throttle bookkeeping.
@Entity(tableName = "Settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
)
