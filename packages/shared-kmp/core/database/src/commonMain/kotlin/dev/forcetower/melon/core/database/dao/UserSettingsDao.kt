package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM UserSettings LIMIT 1")
    fun observeCurrent(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM UserSettings LIMIT 1")
    suspend fun getCurrent(): UserSettingsEntity?

    @Upsert
    suspend fun upsert(entity: UserSettingsEntity)

    // Per-column updates so an optimistic toggle on the iOS side hits a
    // single column rather than rewriting the whole row. Each field stays a
    // Kotlin nullable: null = leave alone. The COALESCE pattern mirrors the
    // backend's PATCH semantics.
    @Query(
        """
        UPDATE UserSettings SET
            gradeSpoiler = COALESCE(:gradeSpoiler, gradeSpoiler),
            notifMsgBroadcast = COALESCE(:notifMsgBroadcast, notifMsgBroadcast),
            notifMsgClass = COALESCE(:notifMsgClass, notifMsgClass),
            notifMsgDirect = COALESCE(:notifMsgDirect, notifMsgDirect),
            notifGradePosted = COALESCE(:notifGradePosted, notifGradePosted),
            notifGradeChanged = COALESCE(:notifGradeChanged, notifGradeChanged),
            notifGradeDateChanged = COALESCE(:notifGradeDateChanged, notifGradeDateChanged),
            notifClassLocation = COALESCE(:notifClassLocation, notifClassLocation),
            notifClassMaterial = COALESCE(:notifClassMaterial, notifClassMaterial),
            notifClassSubject = COALESCE(:notifClassSubject, notifClassSubject)
        WHERE userId = :userId
        """,
    )
    suspend fun patch(
        userId: String,
        gradeSpoiler: Int?,
        notifMsgBroadcast: Boolean?,
        notifMsgClass: Boolean?,
        notifMsgDirect: Boolean?,
        notifGradePosted: Boolean?,
        notifGradeChanged: Boolean?,
        notifGradeDateChanged: Boolean?,
        notifClassLocation: Boolean?,
        notifClassMaterial: Boolean?,
        notifClassSubject: Boolean?,
    )

    @Query("DELETE FROM UserSettings")
    suspend fun clear()
}
