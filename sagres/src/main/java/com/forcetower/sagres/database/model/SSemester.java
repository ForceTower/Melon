/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.database.model;

import com.forcetower.sagres.database.Timestamped;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class SSemester implements Comparable<SSemester>, Timestamped {
    @SerializedName("id")
    private long uefsId;
    @SerializedName("descricao")
    private String name;
    @SerializedName("codigo")
    private String codename;
    @SerializedName("inicio")
    private String start;
    @SerializedName("fim")
    private String end;
    @SerializedName("inicioAulas")
    private String startClasses;
    @SerializedName("fimAulas")
    private String endClasses;

    public SSemester(long uefsId, String name, String codename, String start, String end, String startClasses, String endClasses) {
        this.uefsId = uefsId;
        this.name = name;
        this.codename = codename;
        this.start = start;
        this.end = end;
        this.startClasses = startClasses;
        this.endClasses = endClasses;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getUefsId() {
        return uefsId;
    }

    public void setUefsId(long uefsId) {
        this.uefsId = uefsId;
    }

    public long getStartInMillis() {
        return getInMillis(getStart(), -1);
    }

    public long getEndInMillis() {
        return getInMillis(getEnd(), -1);
    }

    public long getStartClassesInMillis() {
        return getInMillis(getStartClasses(), -1);
    }

    public long getEndClassesInMillis() {
        return getInMillis(getEndClasses(), -1);
    }

    @Override
    public int compareTo(@NonNull SSemester o) {
        try {
            String o1 = getName();
            String o2 = o.getName();
            int str1 = Integer.parseInt(o1.substring(0, 5));
            int str2 = Integer.parseInt(o2.substring(0, 5));

            if (str1 == str2) {
                if (o1.length() > 5) return -1;
                return 1;
            } else {
                return Integer.compare(str1, str2) * -1;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    @Override
    public String toString() {
        return getName();
    }

    public static SSemester getCurrentSemester(List<SSemester> semesters) {
        if (semesters == null || semesters.isEmpty()) {
            return new SSemester(0, "2018.2", "20182", "", "", "", "");
        }
        Collections.sort(semesters);
        return semesters.get(0);
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStartClasses() {
        return startClasses;
    }

    public void setStartClasses(String startClasses) {
        this.startClasses = startClasses;
    }

    public String getEndClasses() {
        return endClasses;
    }

    public void setEndClasses(String endClasses) {
        this.endClasses = endClasses;
    }
}
