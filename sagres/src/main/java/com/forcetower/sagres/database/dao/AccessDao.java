/*
 * Copyright (c) 2018.
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
