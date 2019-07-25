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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(indices = {
    @Index(value = "sagres_id", unique = true),
    @Index(value = "cpf"),
    @Index(value = "email")
})
public class SPerson {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @SerializedName(value = "nome")
    private String name;
    @SerializedName(value = "nomeExibicao")
    private String exhibitionName;
    private String cpf;
    private String email;
    @ColumnInfo(name = "sagres_id")
    @Nullable
    private String sagresId;
    private boolean mocked;

    public SPerson(long id, String name, String exhibitionName, String cpf, String email) {
        this.id = id;
        this.name = name;
        this.exhibitionName = exhibitionName;
        this.cpf = cpf;
        this.email = email;
        this.mocked = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        name = name.trim();
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExhibitionName() {
        return exhibitionName;
    }

    public void setExhibitionName(String exhibitionName) {
        this.exhibitionName = exhibitionName;
    }

    public String getCpf() {
        if (cpf == null) return null;

        cpf = cpf.trim();
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    @NonNull
    public String toString() {
        return "ID: " + getId() + " - Name: " + getName();
    }

    @Nullable
    public String getSagresId() {
        return sagresId;
    }

    public void setSagresId(@Nullable String sagresId) {
        this.sagresId = sagresId;
    }

    public String getUnique() {
        return cpf.toLowerCase() + ".." + id;
    }

    public boolean isMocked() {
        return mocked;
    }

    public void setMocked(boolean mocked) {
        this.mocked = mocked;
    }
}
