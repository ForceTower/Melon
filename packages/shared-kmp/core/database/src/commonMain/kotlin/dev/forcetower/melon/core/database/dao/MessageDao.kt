package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MessageDao {
    @Query("SELECT * FROM Message ORDER BY timestamp DESC, id DESC")
    abstract fun observeInbox(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageState WHERE messageId = :messageId")
    abstract suspend fun getState(messageId: String): MessageStateEntity?

    @Query("SELECT * FROM MessageState WHERE messageId IN (:ids)")
    abstract fun observeStates(ids: List<String>): Flow<List<MessageStateEntity>>

    @Upsert
    abstract suspend fun upsertMessages(messages: List<MessageEntity>)

    @Upsert
    abstract suspend fun upsertScopes(scopes: List<MessageScopeEntity>)

    @Upsert
    abstract suspend fun upsertAttachments(attachments: List<MessageAttachmentEntity>)

    @Upsert
    abstract suspend fun upsertState(state: MessageStateEntity)

    // Scopes + attachments get wiped per message before reinsert so the local
    // graph reflects the server's view (deletes propagate). Messages themselves
    // are upsert-only — nothing on the server deletes them.
    @Query("DELETE FROM MessageScope WHERE messageId IN (:messageIds)")
    abstract suspend fun deleteScopesFor(messageIds: List<String>)

    @Query("DELETE FROM MessageAttachment WHERE messageId IN (:messageIds)")
    abstract suspend fun deleteAttachmentsFor(messageIds: List<String>)

    @Transaction
    open suspend fun applyMessagePage(
        messages: List<MessageEntity>,
        scopes: List<MessageScopeEntity>,
        attachments: List<MessageAttachmentEntity>,
    ) {
        if (messages.isEmpty()) return
        val ids = messages.map { it.id }
        deleteScopesFor(ids)
        deleteAttachmentsFor(ids)
        upsertMessages(messages)
        upsertScopes(scopes)
        upsertAttachments(attachments)
    }

    @Query("DELETE FROM Message")
    abstract suspend fun clear()
}
