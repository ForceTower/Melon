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
