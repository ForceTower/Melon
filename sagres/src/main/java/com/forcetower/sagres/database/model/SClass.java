/*
 * Copyright (c) 2019.
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
