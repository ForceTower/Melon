package com.forcetower.sagres.database;

import android.content.Context;

import com.forcetower.sagres.database.model.SagresAccess;

import androidx.annotation.RestrictTo;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        SagresAccess.class
}, version = 1)
public abstract class SagresDatabase extends RoomDatabase {
    private static final String DB_NAME = "unesx_sagres_database.db";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static SagresDatabase create(Context context) {
        return Room.databaseBuilder(context, SagresDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    public abstract AccessDao accessDao();
}
