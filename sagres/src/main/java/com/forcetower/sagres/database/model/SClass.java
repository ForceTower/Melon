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
public class SClass {
    @PrimaryKey
    private long id;
    @ColumnInfo(name = "description")
    @SerializedName(value = "descricao")
    private String description;
    @SerializedName(value = "tipo")
    private String kind;
    private String link;
    @ColumnInfo(name = "discipline_link")
    private String disciplineLink;

    @Ignore
    @SerializedName(value = "atividadeCurricular")
    private SLinker discipline;

    public SClass(long id, String description, String kind, String link, String disciplineLink) {
        this.id = id;
        this.description = description;
        this.kind = kind;
        this.link = link;
        this.disciplineLink = disciplineLink;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDisciplineLink() {
        return disciplineLink;
    }

    public void setDisciplineLink(String disciplineLink) {
        this.disciplineLink = disciplineLink;
    }

    public SLinker getDiscipline() {
        return discipline;
    }

    public void setDiscipline(SLinker discipline) {
        this.discipline = discipline;
    }
}
