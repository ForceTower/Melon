package com.forcetower.uefs.core.storage.repository.cloud

import androidx.room.withTransaction
import com.forcetower.uefs.core.model.unes.EdgeAppMessage
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import dagger.Reusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.ZoneId
import javax.inject.Inject

@Reusable
class EdgeMessageRepository @Inject constructor(
    private val service: EdgeService,
    private val database: UDatabase
) {
    suspend fun syncDataIfNeeded() {
        database.edgeAccessToken.require() ?: return
        messages()
    }

    fun getAll(): Flow<List<EdgeAppMessage>> {
        return database.edgeMessages.getAll()
    }

    private suspend fun messages() {
        val messages = service.appMessages().data
        val converted = messages.map {
            EdgeAppMessage(
                it.id,
                it.title,
                it.content,
                it.imageUrl,
                it.clickableLink,
                it.createdAt.atZoneSameInstant(ZoneId.systemDefault())
            )
        }
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.edgeMessages.deleteAll()
                database.edgeMessages.insertAllIgnore(converted)
            }
        }
    }
}
