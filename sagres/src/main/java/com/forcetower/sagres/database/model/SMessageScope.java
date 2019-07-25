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
    @Index(value = {"sagres_id"}, unique = true)
})
public class SMessageScope {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    @ColumnInfo(name = "sagres_id")
    private String sagresId;
    @ColumnInfo(name = "clazz_link")
    private String clazzLink;
    @Ignore
    @SerializedName(value = "classe")
    private SLinker clazzLinker;

    public SMessageScope(long uid, String sagresId, String clazzLink) {
        this.uid = uid;
        this.sagresId = sagresId;
        this.clazzLink = clazzLink;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getSagresId() {
        return sagresId;
    }

    public void setSagresId(String sagresId) {
        this.sagresId = sagresId;
    }

    public String getClazzLink() {
        return clazzLink;
    }

    public void setClazzLink(String clazzLink) {
        this.clazzLink = clazzLink;
    }

    public SLinker getClazzLinker() {
        return clazzLinker;
    }

    public void setClazzLinker(SLinker clazzLinker) {
        this.clazzLinker = clazzLinker;
    }
}
