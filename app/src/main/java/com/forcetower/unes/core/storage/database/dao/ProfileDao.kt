package com.forcetower.unes.core.storage.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.forcetower.sagres.database.model.Person
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
    fun insert(person: Person) {
        var profile = selectMeDirect()
        if (profile != null) {
            updateProfile(person.name, person.email)
        } else {
            profile = Profile(name = person.name, email = person.email, sagresId = person.id, me = true)
            insert(profile)
        }
    }

    @Query("UPDATE Profile SET name = :name, email = :email WHERE me = 1")
    abstract fun updateProfile(name: String, email: String)
}