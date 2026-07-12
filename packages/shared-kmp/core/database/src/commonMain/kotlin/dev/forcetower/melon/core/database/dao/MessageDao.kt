package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.forcetower.melon.core.database.query.UnreadMessageHeadRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MessageDao {
    @Query("SELECT * FROM Message ORDER BY timestamp DESC, id DESC")
    abstract fun observeInbox(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM Message WHERE id = :id")
    abstract fun observeMessage(id: String): Flow<MessageEntity?>

    @Query("SELECT * FROM MessageScope")
    abstract fun observeAllScopes(): Flow<List<MessageScopeEntity>>

    @Query("SELECT * FROM MessageAttachment ORDER BY position ASC")
    abstract fun observeAllAttachments(): Flow<List<MessageAttachmentEntity>>

    @Query("SELECT * FROM MessageState")
    abstract fun observeAllStates(): Flow<List<MessageStateEntity>>

    @Query("SELECT * FROM MessageScope WHERE messageId = :messageId")
    abstract fun observeScopesFor(messageId: String): Flow<List<MessageScopeEntity>>

    @Query("SELECT * FROM MessageAttachment WHERE messageId = :messageId ORDER BY position ASC")
    abstract fun observeAttachmentsFor(messageId: String): Flow<List<MessageAttachmentEntity>>

    @Query("SELECT * FROM Message WHERE id = :id")
    abstract suspend fun getMessage(id: String): MessageEntity?

    @Query("SELECT * FROM MessageState WHERE messageId = :messageId")
    abstract suspend fun getState(messageId: String): MessageStateEntity?

    // Mirrored server value only — display OR-merges it with the overlay, so
    // an unstar must also clear it or it would stay shadowed until the ack
    // round-trips through a refresh (matches iOS `setMessageStarred`).
    @Query("UPDATE Message SET starred = :starred WHERE id = :id")
    abstract suspend fun updateMessageStarred(id: String, starred: Boolean)

    @Query("SELECT * FROM MessageState WHERE messageId IN (:ids)")
    abstract fun observeStates(ids: List<String>): Flow<List<MessageStateEntity>>

    @Query("SELECT * FROM MessageState WHERE messageId IN (:ids)")
    abstract suspend fun getStates(ids: List<String>): List<MessageStateEntity>

    // Unread = server hasn't marked it read AND no local overlay has either
    // (a missing MessageState row counts as unread). Matches iOS
    // `MirrorStore.messageItem`: read != true && readAt == null.
    @Query(
        """
        SELECT m.id FROM Message m
          LEFT JOIN MessageState ms ON ms.messageId = m.id
         WHERE m.read IS NOT 1 AND ms.readAt IS NULL
        """,
    )
    abstract suspend fun unreadMessageIds(): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM Message m
          LEFT JOIN MessageState ms ON ms.messageId = m.id
         WHERE m.read IS NOT 1 AND ms.readAt IS NULL
        """,
    )
    abstract fun observeUnreadCount(): Flow<Int>

    // Latest message head, read or unread — the Overview "recados" tile always
    // surfaces the most recent message; the unread count drives its badge.
    @Query(
        """
        SELECT m.senderName AS senderName,
               m.subject AS subject,
               m.content AS content,
               m.timestamp AS timestamp
          FROM Message m
         ORDER BY m.timestamp DESC, m.id DESC
         LIMIT 1
        """,
    )
    abstract fun observeLatestHead(): Flow<UnreadMessageHeadRow?>

    @Upsert
    abstract suspend fun upsertMessages(messages: List<MessageEntity>)

    @Upsert
    abstract suspend fun upsertScopes(scopes: List<MessageScopeEntity>)

    @Upsert
    abstract suspend fun upsertAttachments(attachments: List<MessageAttachmentEntity>)

    @Upsert
    abstract suspend fun upsertState(state: MessageStateEntity)

    @Upsert
    abstract suspend fun upsertStates(states: List<MessageStateEntity>)

    // Atomically flips readAt to `now` for every currently-unread message,
    // preserving each existing row's `starred` flag. Idempotent: a second
    // call inside the same `now` window is a no-op because `unreadMessageIds`
    // already filters them out.
    @Transaction
    open suspend fun markAllRead(now: String) {
        val ids = unreadMessageIds()
        if (ids.isEmpty()) return
        val existing = getStates(ids).associateBy { it.messageId }
        upsertStates(
            ids.map { id ->
                val cur = existing[id]
                MessageStateEntity(
                    messageId = id,
                    readAt = now,
                    starred = cur?.starred == true,
                    updatedAt = now,
                )
            },
        )
    }

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
