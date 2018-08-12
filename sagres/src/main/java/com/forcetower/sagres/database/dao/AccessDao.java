package com.forcetower.sagres.database.dao;

import com.forcetower.sagres.database.model.SagresAccess;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface AccessDao {
    @Query("SELECT * FROM SagresAccess LIMIT 1")
    LiveData<SagresAccess> getAccess();

    @Query("SELECT * FROM SagresAccess LIMIT 1")
    SagresAccess getAccessDirect();

    @Insert
    void insert(SagresAccess access);
}
