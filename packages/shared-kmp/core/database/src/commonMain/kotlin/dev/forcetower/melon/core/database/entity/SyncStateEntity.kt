package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// K/V bookkeeping for the sync layer: last-active-semester-pull timestamp,
// messages cursor, onboarding-complete flag. Separate from Settings so a
// "reset sync" action can nuke just this table.
@Entity(tableName = "SyncState")
data class SyncStateEntity(
    @PrimaryKey val key: String,
    val value: String,
)
