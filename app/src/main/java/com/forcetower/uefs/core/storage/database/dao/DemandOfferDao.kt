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

package com.forcetower.uefs.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.uefs.core.model.unes.SDemandOffer

@Dao
abstract class DemandOfferDao {
    @Insert(onConflict = REPLACE)
    protected abstract fun insert(offers: List<SDemandOffer>)

    @Query("DELETE FROM SDemandOffer")
    abstract fun deleteAll()

    @Query("SELECT * FROM SDemandOffer ORDER BY category, code ASC")
    abstract fun getAll(): LiveData<List<SagresDemandOffer>>

    @Transaction
    open fun defineDemandOffers(offers: List<SagresDemandOffer>) {
        deleteAll()
        insert(offers.map { SDemandOffer.fromSagres(it) })
    }

    @Query("UPDATE SDemandOffer SET selected = :select WHERE uid = :uid")
    abstract fun updateOfferSelection(uid: Long, select: Boolean)

    @Query("SELECT * FROM SDemandOffer ORDER BY category, code ASC")
    abstract fun getAllDirect(): List<SagresDemandOffer>

    @Query("SELECT * FROM SDemandOffer WHERE selected = 1")
    abstract fun getSelected(): LiveData<List<SagresDemandOffer>>
}
