package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "MessageScope",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("messageId")],
)
data class MessageScopeEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val scope: String, // university | coordination | course | class | personal | list
    val classId: String?,
    val courseId: String?,
    val studentId: String?,
    val platformScopeId: String?,
    val disciplineCode: String?,
    val disciplineName: String?,
)
