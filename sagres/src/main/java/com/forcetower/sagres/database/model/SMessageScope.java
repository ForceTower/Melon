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
