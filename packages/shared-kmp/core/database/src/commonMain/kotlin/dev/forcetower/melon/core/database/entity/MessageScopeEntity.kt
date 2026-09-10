package dev.forcetower.melon.core.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
