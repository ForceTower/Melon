package com.forcetower.sagres.database;

import com.forcetower.sagres.database.model.SagresAccess;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface AccessDao {
    @Query("SELECT * FROM SagresAccess LIMIT 1")
    LiveData<SagresAccess> getAccess();
}
