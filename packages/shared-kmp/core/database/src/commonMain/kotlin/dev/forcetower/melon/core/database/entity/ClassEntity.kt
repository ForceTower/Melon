package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
