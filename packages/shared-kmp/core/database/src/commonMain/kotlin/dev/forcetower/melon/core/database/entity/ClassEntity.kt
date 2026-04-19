package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Class",
    foreignKeys = [
        ForeignKey(
            entity = DisciplineOfferEntity::class,
            parentColumns = ["id"],
            childColumns = ["offerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("offerId")],
)
data class ClassEntity(
    @PrimaryKey val id: String,
    val offerId: String,
    val platformId: Long,
    val groupName: String,
    val type: String,
    val hours: Int,
    val program: String?,
)
