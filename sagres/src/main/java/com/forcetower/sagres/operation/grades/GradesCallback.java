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

package com.forcetower.sagres.operation.grades;

import com.forcetower.sagres.database.model.DisciplineMissedClass;
import com.forcetower.sagres.database.model.Grade;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Pair;

public class GradesCallback extends BaseCallback<GradesCallback> {
    private List<DisciplineMissedClass> frequency;
    private List<Grade> grades;
    private List<Pair<Long, String>> semesters;

    public GradesCallback(@NonNull Status status) {
        super(status);
    }

    public GradesCallback grades(@Nullable List<Grade> grades) {
        this.grades = grades;
        return this;
    }

   public GradesCallback frequency(@Nullable List<DisciplineMissedClass> frequency) {
        this.frequency = frequency;
        return this;
   }

   public GradesCallback codes(@Nullable List<Pair<Long, String>> semesters) {
        this.semesters = semesters;
        return this;
   }

    public List<DisciplineMissedClass> getFrequency() {
        return frequency;
    }

    public List<Grade> getGrades() {
        return grades;
    }

    public List<Pair<Long, String>> getSemesters() {
        return semesters;
    }
}
