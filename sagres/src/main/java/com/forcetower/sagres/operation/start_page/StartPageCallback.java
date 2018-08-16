package com.forcetower.sagres.operation.start_page;

import com.forcetower.sagres.database.model.Discipline;
import com.forcetower.sagres.database.model.DisciplineClassLocation;
import com.forcetower.sagres.database.model.DisciplineGroup;
import com.forcetower.sagres.database.model.SagresCalendar;
import com.forcetower.sagres.database.model.Semester;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StartPageCallback extends BaseCallback<StartPageCallback> {
    @Nullable
    private List<SagresCalendar> calendar;
    private List<Semester> semesters;
    private List<Discipline> disciplines;
    private List<DisciplineGroup> groups;
    @Nullable
    private List<DisciplineClassLocation> locations;

    public StartPageCallback(@NonNull Status status) {
        super(status);
    }

    public StartPageCallback calendar(@Nullable List<SagresCalendar> calendar) {
        this.calendar = calendar;
        return this;
    }

    public StartPageCallback semesters(List<Semester> semesters) {
        this.semesters = semesters;
        return this;
    }

    public StartPageCallback disciplines(List<Discipline> disciplines) {
        this.disciplines = disciplines;
        return this;
    }

    public StartPageCallback groups(List<DisciplineGroup> groups) {
        this.groups = groups;
        return this;
    }

    public StartPageCallback locations(@Nullable List<DisciplineClassLocation> locations) {
        this.locations = locations;
        return this;
    }

    @Nullable
    public List<SagresCalendar> getCalendar() {
        return calendar;
    }

    public List<Semester> getSemesters() {
        return semesters;
    }

    public List<Discipline> getDisciplines() {
        return disciplines;
    }

    public List<DisciplineGroup> getGroups() {
        return groups;
    }

    @Nullable
    public List<DisciplineClassLocation> getLocations() {
        return locations;
    }
}
