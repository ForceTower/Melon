/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.aeri.core.storage.database.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.forcetower.uefs.aeri.core.model.Announcement
import dev.forcetower.oversee.model.NewsMessage

@Dao
abstract class NewsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(announcement: Announcement)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    abstract fun update(announcement: Announcement)

    @Transaction
    open fun insert(news: List<NewsMessage>) {
        news.mapNotNull {
            val existent = findByLink(it.link)
            if (existent == null) {
                Announcement(0, it.link, it.title, it.imageUrl, it.publishDate, false)
            } else {
                update(
                    existent.copy(
                        title = it.title,
                        imageUrl = it.imageUrl,
                        publishDate = it.publishDate
                    )
                )
                null
            }
        }.forEach {
            insert(it)
        }
    }

    @Query("SELECT * FROM Announcement WHERE link = :link LIMIT 1")
    abstract fun findByLink(link: String): Announcement?

    @Query("SELECT * FROM Announcement ORDER BY id DESC")
    abstract fun getAnnouncementsPaged(): DataSource.Factory<Int, Announcement>

    @Query("SELECT * FROM Announcement WHERE notified = 0")
    abstract suspend fun getNewAnnouncements(): List<Announcement>

    @Query("UPDATE Announcement SET notified = 1")
    abstract suspend fun markAllNotified()

    @Query("DELETE FROM Announcement")
    abstract suspend fun deleteAll()
}
