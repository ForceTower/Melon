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

package com.forcetower.sagres.parsers;

import android.util.SparseArray;

import com.forcetower.sagres.database.model.SDisciplineClassLocation;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.forcetower.sagres.utils.DateUtils.getDayOfWeek;

/**
 * Created by João Paulo on 07/03/2018.
 */

public class SagresScheduleParser {

    private static SparseArray<String> iterationPerDay;
    private static HashMap<String, SagresClass> codePerLessons;

    public static List<SDisciplineClassLocation> getSchedule(@NonNull Document document) {
        Element schedule = document.selectFirst("table[class=\"meus-horarios\"]");
        Element subtitle = document.selectFirst("table[class=\"meus-horarios-legenda\"]");

        if (schedule == null) {
            Timber.d("Schedule not found! Prob is \"Schedule Undefined\"");
            return null;
        }

        iterationPerDay = new SparseArray<>();
        codePerLessons = new HashMap<>();

        findSchedule(schedule);
        findDetails(subtitle);
        HashMap<String, List<SagresClassDay>> classDay = getSchedule(codePerLessons);
        return convertToNewType(classDay);
    }

    private static List<SDisciplineClassLocation> convertToNewType(HashMap<String, List<SagresClassDay>> hashMap) {
        List<SDisciplineClassLocation> disciplineClassLocations = new ArrayList<>();
        for (String key : hashMap.keySet()) {
            List<SagresClassDay> classDays = hashMap.get(key);
            if (classDays == null)
                continue;

            for (SagresClassDay classDay : classDays) {
                SDisciplineClassLocation location = new SDisciplineClassLocation(
                        classDay.starts_at,
                        classDay.ends_at,
                        classDay.day,
                        classDay.room,
                        classDay.campus,
                        classDay.modulo,
                        classDay.class_name,
                        classDay.class_code,
                        classDay.class_type
                );
                Timber.d("Class Code %s, Class Name: %s, Room %s", classDay.class_code, classDay.class_name, classDay.room);
                Timber.d("Class type: %s", classDay.class_type);
                disciplineClassLocations.add(location);
            }
        }

        return disciplineClassLocations;
    }

    private static void findSchedule(Element schedule) {
        Elements trs = schedule.select("tr");

        for (int i = 0; i < trs.size(); i++) {
            Element tr = trs.get(i);
            if (i == 0) {
                //Header -> days of class
                Elements ths = tr.select("th");
                for (int j = 0; j < ths.size(); j++) {
                    Element th = ths.get(j);
                    if (!th.text().trim().isEmpty()) {
                        iterationPerDay.put(j, th.text().trim());
                    }
                }
            } else {
                Elements tds = tr.select("td");
                String start = "";
                String end = "";
                for (int j = 0; j < tds.size(); j++) {
                    Element td = tds.get(j);

                    String classTime = td.text().trim();
                    if (classTime.trim().isEmpty()) {
                        continue;
                    }

                    String[] parts = classTime.split(" ");
                    String one = parts[0].trim();
                    String two = parts[1].trim();

                    if (j == 0) {
                        start = one;
                        end = two;
                    } else {
                        String day = iterationPerDay.get(j);
                        SagresClass clazz = codePerLessons.get(one);

                        if (clazz == null) clazz = new SagresClass(one);

                        clazz.addClazz(two);
                        clazz.addStartEndTime(start, end, day, two);

                        codePerLessons.put(one, clazz);
                    }
                }
            }
        }
    }

    private static void findDetails(Element subtitle) {
        Elements trs = subtitle.select("tr");

        String currentCode = "undef";
        for (int i = 0; i < trs.size(); i++) {
            Element tr = trs.get(i);
            Elements tds = tr.select("td");
            String value = tds.get(1).text();

            Element td = tds.get(0);
            if (td.html().contains("&nbsp;")) {
                String[] parts = value.split("::");
                if (parts.length == 2) {
                    if (!currentCode.equals("undef")) {
                        SagresClass lesson = codePerLessons.get(currentCode);
                        if (lesson != null) lesson.addAtToAllClasses(parts[0].trim(), parts[1].trim());
                        else {
                            Timber.d("Something wrong is happening here...");
                        }
                    }
                } else if (parts.length == 3) {
                    if (!currentCode.equals("undef")) {
                        SagresClass lesson = codePerLessons.get(currentCode);
                        if (lesson != null) lesson.addAtToSpecificClass(parts[2].trim(), parts[1].trim(), parts[0].trim());
                        else {
                            Timber.d("Something wrong is happening here...");
                        }
                    }
                }
            } else {
                int splitPos = value.indexOf("-");
                String code = value.substring(0, splitPos).trim();
                String name = value.substring(splitPos + 1).trim();

                currentCode = code;
                SagresClass lesson = codePerLessons.get(code);
                if (lesson != null) {
                    lesson.setName(name);
                    codePerLessons.put(code, lesson);
                } else {
                    Timber.d("Something was ignored due to a bug. Since this might be changed leave as is");
                }
            }
        }
    }

    private static HashMap<String, List<SagresClassDay>> getSchedule(HashMap<String, SagresClass> classes) {
        HashMap<String, List<SagresClassDay>> classPerDay = new HashMap<>();

        if (classes.isEmpty())
            return classPerDay;

        for (int i = 1; i <= 7; i++) {
            String dayOfWeek = getDayOfWeek(i);
            List<SagresClassDay> dayOfClass = new ArrayList<>();

            for (SagresClass uClass : classes.values()) {
                List<SagresClassDay> days = uClass.getDays();

                for (SagresClassDay clazz : days) {
                    if (clazz.getDay().equalsIgnoreCase(dayOfWeek)) {
                        dayOfClass.add(clazz);
                    }
                }
            }

            if (!dayOfClass.isEmpty()) classPerDay.put(dayOfWeek, dayOfClass);
        }

        return classPerDay;
    }

    private static class SagresClass {
        private String name;
        private String code;
        private List<String> classes;
        private List<SagresClassDay> days;

        SagresClass(String code) {
            this.code = code;
            classes = new ArrayList<>();
            days = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            for (SagresClassDay classDay : days) {
                classDay.setClassName(name);
            }
        }

        public String getCode() {
            return code;
        }

        void addClazz(String aClass) {
            aClass = aClass.trim();
            if (!containsClazz(aClass))
                this.classes.add(aClass);
        }

        void addStartEndTime(String start, String finish, String day, String classType) {
            SagresClassDay classDay = new SagresClassDay(start, finish, day, classType, this);
            days.add(classDay);
        }

        private boolean containsClazz(String clazz) {
            return classes.contains(clazz);
        }

        void addAtToAllClasses(String type, String at) {
            String[] parts = at.split(",");

            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }

            String allocatedRoom;
            String place;
            String campus;

            if (parts.length == 3) {
                campus = parts[0];
                place = parts[1];
                allocatedRoom = removeRoomName(parts[2]);
            } else if (parts.length == 2) {
                campus = parts[0];
                allocatedRoom = removeRoomName(parts[1]);
                place = "";
            } else {
                allocatedRoom = removeRoomName(parts[0]);
                campus = "";
                place = "";
            }


            Timber.d("All add -----------");
            for (SagresClassDay classDay : days) {
                if (classDay.getClassType().equalsIgnoreCase(type)) {
                    Timber.d("Kappa");
                    classDay.setRoom(allocatedRoom);
                    classDay.setCampus(campus);
                    classDay.setModulo(place);
                }
            }
            Timber.d("End of all add ---------");
        }

        void addAtToSpecificClass(String at, String day, String type) {
            String[] parts = at.split(",");

            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }

            String allocatedRoom;
            String place;
            String campus;

            if (parts.length == 3) {
                campus = parts[0];
                place = parts[1];
                allocatedRoom = removeRoomName(parts[2]);
            } else if (parts.length == 2) {
                campus = parts[0];
                allocatedRoom = removeRoomName(parts[1]);
                place = "";
            } else {
                allocatedRoom = removeRoomName(parts[0]);
                campus = "";
                place = "";
            }

            Timber.d("Specific add ------");
            for (SagresClassDay classDay : days) {
                if (classDay.getDay().equals(day) && classDay.getClassType().equals(type)) {
                    Timber.d("%s %s %s", classDay.class_code, classDay.class_type, allocatedRoom);
                    classDay.setRoom(allocatedRoom);
                    classDay.setCampus(campus);
                    classDay.setModulo(place);
                }
            }
            Timber.d("End of specific -------");
        }

        private String removeRoomName(String part) {
            if (part.startsWith("Sala")) {
                part = part.substring(4);
            }

            return part.trim();
        }

        List<SagresClassDay> getDays() {
            return days;
        }
    }

    private static class SagresClassDay {
        private String starts_at;
        private String ends_at;
        private String class_type;
        private String day;
        private String room;
        private String campus;
        private String modulo;
        private String class_code;
        private String class_name;

        String getClassType() {
            return class_type;
        }

        String getDay() {
            return day;
        }

        public String getRoom() {
            return room;
        }

        public void setRoom(String room) {
            Timber.d("Room of %s set to %s from %s", class_code, room, this.room);
            this.room = room;
        }

        void setCampus(String campus) {
            this.campus = campus;
        }

        void setModulo(String modulo) {
            this.modulo = modulo;
        }

        void setClassName(String class_name) {
            this.class_name = class_name;
        }

        SagresClassDay(String class_code, String class_name, String starts_at, String ends_at, String class_type, String day, String room, String campus, String modulo) {
            this.starts_at = starts_at;
            this.ends_at = ends_at;
            this.class_type = class_type;
            this.day = day;
            this.room = room;
            this.campus = campus;
            this.modulo = modulo;
            this.class_code = class_code;
            this.class_name = class_name;
        }

        SagresClassDay(String start, String finish, String day, String classType, SagresClass sagresClass) {
            this(sagresClass.getCode(), sagresClass.getName(), start, finish, classType, day, null, null, null);
        }
    }
}
