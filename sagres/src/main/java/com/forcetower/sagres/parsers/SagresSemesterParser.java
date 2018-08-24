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

package com.forcetower.sagres.parsers;

import com.forcetower.sagres.database.model.SSemester;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

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