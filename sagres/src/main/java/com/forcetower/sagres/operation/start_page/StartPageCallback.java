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

package com.forcetower.sagres.operation.start_page;

import com.forcetower.sagres.database.model.SDiscipline;
import com.forcetower.sagres.database.model.SDisciplineClassLocation;
import com.forcetower.sagres.database.model.SDisciplineGroup;
import com.forcetower.sagres.database.model.SCalendar;
import com.forcetower.sagres.database.model.SSemester;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StartPageCallback extends BaseCallback<StartPageCallback> {
    @Nullable
    private List<SCalendar> calendar;
    private List<SSemester> semesters;
    private List<SDiscipline> disciplines;
    private List<SDisciplineGroup> groups;
    @Nullable
    private List<SDisciplineClassLocation> locations;

    public StartPageCallback(@NonNull Status status) {
        super(status);
    }

    public StartPageCallback calendar(@Nullable List<SCalendar> calendar) {
        this.calendar = calendar;
        return this;
    }

    public StartPageCallback semesters(List<SSemester> semesters) {
        this.semesters = semesters;
        return this;
    }

    public StartPageCallback disciplines(List<SDiscipline> disciplines) {
        this.disciplines = disciplines;
        return this;
    }

    public StartPageCallback groups(List<SDisciplineGroup> groups) {
        this.groups = groups;
        return this;
    }

    public StartPageCallback locations(@Nullable List<SDisciplineClassLocation> locations) {
        this.locations = locations;
        return this;
    }

    @Nullable
    public List<SCalendar> getCalendar() {
        return calendar;
    }

    public List<SSemester> getSemesters() {
        return semesters;
    }

    public List<SDiscipline> getDisciplines() {
        return disciplines;
    }

    public List<SDisciplineGroup> getGroups() {
        return groups;
    }

    @Nullable
    public List<SDisciplineClassLocation> getLocations() {
        return locations;
    }
}
