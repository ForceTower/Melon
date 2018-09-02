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

package com.forcetower.sagres.database.model;

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

    public SDisciplineGroup(String teacher, String group, int credits, int missLimit, String classPeriod, String department) {
        this.teacher = teacher;
        this.group = group;
        this.credits = credits;
        this.missLimit = missLimit;
        this.classPeriod = classPeriod;
        this.department = department;
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
        if (other.credits > 0   || credits == 0)    credits     = other.credits;
        if (other.missLimit > 0 || missLimit == 0)  missLimit   = other.missLimit;
    }

    @Override
    public String toString() {
        return group + "";
    }

    public int getIgnored() {
        return ignored;
    }

    public void setIgnored(int ignored) {
        this.ignored = ignored;
    }
}
