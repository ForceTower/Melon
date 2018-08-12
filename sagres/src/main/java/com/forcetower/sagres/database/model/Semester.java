package com.forcetower.sagres.database.model;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Semester implements Comparable<Semester> {
    private String uefsId;
    private String name;

    public Semester(String uefsId, String name) {
        this.uefsId = uefsId;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUefsId() {
        return uefsId;
    }

    public void setUefsId(String uefsId) {
        this.uefsId = uefsId;
    }

    @Override
    public int compareTo(@NonNull Semester o) {
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

    @Override
    public String toString() {
        return getName();
    }

    public static Semester getCurrentSemester(List<Semester> semesters) {
        if (semesters == null || semesters.isEmpty()) {
            return new Semester("0", "20182");
        }
        Collections.sort(semesters);
        return semesters.get(0);
    }
}
