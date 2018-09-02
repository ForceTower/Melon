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
