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

package com.forcetower.sagres.parsers;

import com.forcetower.sagres.database.model.SSemester;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

public class SagresSemesterParser {

    public static List<SSemester> getSemesters(Document document) {
        List<SSemester> semesters = new ArrayList<>();
        Elements classes = document.select("section[class=\"webpart-aluno-item\"]");

        List<String> strings = new ArrayList<>();
        for (Element element : classes) {
            String period = element.selectFirst("span[class=\"webpart-aluno-periodo\"]").text();
            period = period.toLowerCase();
            if (!strings.contains(period)) strings.add(period);
        }
        Timber.d("Semesters: %s", strings);
        for (int i = 0; i < strings.size(); i++) {
            semesters.add(new SSemester(strings.size() - i, strings.get(i), strings.get(i), "", "", "", ""));
        }

        return semesters;
    }
}