package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "DisciplineOffer",
    foreignKeys = [
        ForeignKey(
            entity = DisciplineEntity::class,
            parentColumns = ["id"],
            childColumns = ["disciplineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("disciplineId"), Index("semesterId")],
)
data class DisciplineOfferEntity(
    @PrimaryKey val id: String,
    val disciplineId: String,
    val semesterId: String,
    val platformId: Long,
    val hours: Int?,
    val program: String?,
)
