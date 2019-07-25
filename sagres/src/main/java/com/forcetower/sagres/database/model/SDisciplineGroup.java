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

package com.forcetower.sagres.database.model;

import androidx.annotation.Nullable;

import java.util.List;

import static com.forcetower.sagres.utils.WordUtils.validString;

public class SDisciplineGroup {
    private String teacher;
    private String group;
    private int credits;
    private int missLimit;
    private String classPeriod;
    private String department;
    private boolean draft = true;
    private int ignored = 0;
    private String semester;
    private String code;
    private String name;
    @Nullable
    private List<SDisciplineClassLocation> locations;
    @Nullable
    private List<SDisciplineClassItem> classItems;

    public SDisciplineGroup(String teacher, String group, int credits, int missLimit, String classPeriod, String department, List<SDisciplineClassLocation> locations) {
        this.teacher = teacher;
        this.group = group;
        this.credits = credits;
        this.missLimit = missLimit;
        this.classPeriod = classPeriod;
        this.department = department;
        this.locations = locations;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getMissLimit() {
        return missLimit;
    }

    public void setMissLimit(int missLimit) {
        this.missLimit = missLimit;
    }

    public String getClassPeriod() {
        return classPeriod;
    }

    public void setClassPeriod(String classPeriod) {
        this.classPeriod = classPeriod;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public void setDisciplineCodeAndSemester(String code, String semester) {
        this.code = code;
        this.semester = semester;
    }

    public String getSemester() {
        return semester;
    }

    public String getCode() {
        return code;
    }

    public void selectiveCopy(SDisciplineGroup other) {
        if (other == null) return;

        if (validString(other.teacher)      || teacher == null)     teacher     = other.teacher;
        if (validString(other.group)        || group == null)       group       = other.group;
        if (validString(other.classPeriod)  || classPeriod == null) classPeriod = other.classPeriod;
        if (validString(other.department)   || department == null)  department  = other.department;
        if (other.credits > 0               || credits == 0)        credits     = other.credits;
        if (other.missLimit > 0             || missLimit == 0)      missLimit   = other.missLimit;
    }

    public List<SDisciplineClassLocation> getLocations() {
        return locations;
    }

    @Override
    public String toString() {
        return code + ":" + group + "::" + name;
    }

    public int getIgnored() {
        return ignored;
    }

    public void setIgnored(int ignored) {
        this.ignored = ignored;
    }

    public List<SDisciplineClassItem> getClassItems() {
        return classItems;
    }

    public void setClassItems(List<SDisciplineClassItem> classItems) {
        this.classItems = classItems;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
