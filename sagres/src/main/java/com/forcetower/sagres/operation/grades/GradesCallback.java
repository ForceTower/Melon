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

package com.forcetower.sagres.operation.grades;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.forcetower.sagres.database.model.SDisciplineMissedClass;
import com.forcetower.sagres.database.model.SGrade;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;
import kotlin.Pair;

import java.util.List;

public class GradesCallback extends BaseCallback<GradesCallback> {
    @Nullable
    private List<SDisciplineMissedClass> frequency;
    private List<SGrade> grades;
    private List<Pair<Long, String>> semesters;

    public GradesCallback(@NonNull Status status) {
        super(status);
    }

    public GradesCallback grades(@Nullable List<SGrade> grades) {
        this.grades = grades;
        return this;
    }

   public GradesCallback frequency(@Nullable List<SDisciplineMissedClass> frequency) {
        this.frequency = frequency;
        return this;
   }

   public GradesCallback codes(@Nullable List<Pair<Long, String>> semesters) {
        this.semesters = semesters;
        return this;
   }

   @Nullable
    public List<SDisciplineMissedClass> getFrequency() {
        return frequency;
    }

    public List<SGrade> getGrades() {
        return grades;
    }

    public List<Pair<Long, String>> getSemesters() {
        return semesters;
    }
}
