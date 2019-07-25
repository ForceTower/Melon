/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.operation.start_page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.forcetower.sagres.database.model.*;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import java.util.List;

public class StartPageCallback extends BaseCallback<StartPageCallback> {
    @Nullable
    private List<SCalendar> calendar;
    private List<SSemester> semesters;
    private List<SDiscipline> disciplines;
    private List<SDisciplineGroup> groups;
    private List<SMessage> messages;
    @Nullable
    private List<SDisciplineClassLocation> locations;
    private boolean demandOpen = false;

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

    public StartPageCallback demandOpen(boolean demandOpen) {
        this.demandOpen = demandOpen;
        return this;
    }

    public StartPageCallback messages(List<SMessage> messages) {
        this.messages = messages;
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

    public List<SMessage> getMessages() {
        return messages;
    }

    @Nullable
    public List<SDisciplineClassLocation> getLocations() {
        return locations;
    }

    public boolean isDemandOpen() {
        return demandOpen;
    }
}
