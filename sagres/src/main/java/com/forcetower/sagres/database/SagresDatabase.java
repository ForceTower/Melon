/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.database;

import android.content.Context;

import com.forcetower.sagres.database.dao.AccessDao;
import com.forcetower.sagres.database.model.SAccess;

import androidx.annotation.RestrictTo;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        SAccess.class
}, version = 1)
public abstract class SagresDatabase extends RoomDatabase {
    private static final String DB_NAME = "unesx_sagres_database.db";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static SagresDatabase create(Context context) {
        return Room.databaseBuilder(context, SagresDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    public abstract AccessDao accessDao();
}
