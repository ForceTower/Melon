/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.SPerson
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.unes.core.model.unes.Profile

@Dao
abstract class ProfileDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(profile: Profile)

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMeDirect(): Profile?

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMe(): LiveData<Profile>

    @Transaction
    open fun insert(person: SPerson, score: Double = -1.0) {
        val name = WordUtils.capitalize(person.name.trim())
        var profile = selectMeDirect()
        if (profile != null) {
            updateProfile(name, person.email.trim())
            if (score >= 0) updateScore(score)
        } else {
            profile = Profile(name = name, email = person.email.trim(), sagresId = person.id, me = true, score = score)
            insert(profile)
        }
    }

    @Query("UPDATE Profile SET score = :score WHERE me = 1")
    abstract fun updateScore(score: Double)

    @Query("UPDATE Profile SET name = :name, email = :email WHERE me = 1")
    abstract fun updateProfile(name: String, email: String)
}