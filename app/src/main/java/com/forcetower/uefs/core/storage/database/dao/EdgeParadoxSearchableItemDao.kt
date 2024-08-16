package com.forcetower.uefs.core.storage.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.core.database.BaseDao
import com.forcetower.uefs.core.model.edge.paradox.EvaluationSnapshot
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem.Companion.DISCIPLINE_TYPE
import com.forcetower.uefs.core.model.unes.EdgeParadoxSearchableItem.Companion.TEACHER_TYPE
import com.forcetower.uefs.core.util.unaccent
import java.util.Locale

@Dao
abstract class EdgeParadoxSearchableItemDao : BaseDao<EdgeParadoxSearchableItem>() {
    @Transaction
    open suspend fun recreate(snapshot: EvaluationSnapshot) {
        deleteAll()
        val disciplines = snapshot.disciplines.map {
            val coded = it.code
            val search = "$coded ${it.name}".lowercase().unaccent()
            EdgeParadoxSearchableItem(
                serviceId = it.id,
                displayName = it.name,
                subtitle = it.code,
                displayImage = null,
                type = DISCIPLINE_TYPE,
                searchable = search,
                optionalReference = null
            )
        }

        val teachers = snapshot.teachers.map {
            val searchable = it.name.lowercase().unaccent()
            EdgeParadoxSearchableItem(
                serviceId = it.id,
                displayName = it.name,
                subtitle = null,
                displayImage = null,
                type = TEACHER_TYPE,
                searchable = searchable,
                optionalReference = null
            )
        }

        insertAllIgnore(disciplines)
        insertAllIgnore(teachers)
    }

    @Query("DELETE FROM EdgeParadoxSearchableItem")
    abstract suspend fun deleteAll()

    open fun query(query: String): PagingSource<Int, EdgeParadoxSearchableItem> {
        return if (query.isBlank()) {
            doQueryEmpty()
        } else {
            val realQuery = query.lowercase(Locale.getDefault())
                .unaccent()
                .split(" ")
                .joinToString("%", "%", "%")
            doQuery(realQuery)
        }
    }

    @Query("SELECT * FROM EdgeParadoxSearchableItem WHERE LOWER(searchable) LIKE :query ORDER BY displayName")
    abstract fun doQuery(query: String): PagingSource<Int, EdgeParadoxSearchableItem>

    @Query("SELECT * FROM EdgeParadoxSearchableItem WHERE 0")
    abstract fun doQueryEmpty(): PagingSource<Int, EdgeParadoxSearchableItem>
}