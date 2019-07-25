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

import androidx.room.*;
import com.google.gson.annotations.SerializedName;

@Entity(indices = {
    @Index(value = "link", unique = true)
})
public class SDisciplineResumed {
    @PrimaryKey
    private long id;
    @SerializedName(value = "codigo")
    private String code;
    @SerializedName(value = "nome")
    private String name;
    @ColumnInfo(name = "resumed_name")
    @SerializedName(value = "nomeResumido")
    private String resumedName;
    @SerializedName(value = "ementa")
    private String objective;
    @ColumnInfo(name = "department_link")
    private String departmentLink;

    private String link;

    @Ignore
    @SerializedName(value = "departamento")
    private SLinker department;

    public SDisciplineResumed(long id, String code, String name, String resumedName, String objective, String departmentLink, String link) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.resumedName = resumedName;
        this.objective = objective;
        this.departmentLink = departmentLink;
        this.link = link;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResumedName() {
        return resumedName;
    }

    public void setResumedName(String resumedName) {
        this.resumedName = resumedName;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getDepartmentLink() {
        return departmentLink;
    }

    public void setDepartmentLink(String departmentLink) {
        this.departmentLink = departmentLink;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public SLinker getDepartment() {
        return department;
    }

    public void setDepartment(SLinker department) {
        this.department = department;
    }
}
