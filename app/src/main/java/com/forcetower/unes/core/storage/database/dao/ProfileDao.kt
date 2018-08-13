package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.Person
import com.forcetower.sagres.utils.WordUtils
import com.forcetower.unes.core.model.Profile

@Dao
abstract class ProfileDao {
    @Insert(onConflict = REPLACE)
    abstract fun insert(profile: Profile)

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMeDirect(): Profile?

    @Query("SELECT * FROM Profile WHERE me = 1 LIMIT 1")
    abstract fun selectMe(): LiveData<Profile>

    @Transaction
    open fun insert(person: Person, score: Double = -1.0) {
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