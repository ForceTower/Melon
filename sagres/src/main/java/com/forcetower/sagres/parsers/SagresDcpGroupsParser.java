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

import com.forcetower.sagres.database.model.SDisciplineGroup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import static com.forcetower.sagres.utils.ValueUtils.toInteger;

/**
 * Created by João Paulo on 06/03/2018.
 */

public class SagresDcpGroupsParser {

    public static List<SDisciplineGroup> getGroups(Document document) {
        List<SDisciplineGroup> groups = new ArrayList<>();

        Elements disciplines = document.select("section[class=\"webpart-aluno-item\"]");
        for (Element discipline : disciplines) {
            String semester = discipline.selectFirst("span[class=\"webpart-aluno-periodo\"]").text();
            String title = discipline.selectFirst("a[class=\"webpart-aluno-nome cor-destaque\"]").text();
            int codePos = title.indexOf("-");
            String code = title.substring(0, codePos).trim();

            String credits = discipline.select("span[class=\"webpart-aluno-codigo\"]").text();
            credits = credits.replaceAll("[^\\d]", "");


            Element ul = discipline.selectFirst("ul");

            if (ul != null) {
                Elements lis = ul.select("li");
                for (Element li : lis) {
                    Element element = li.selectFirst("a[href]");
                    String type = element.text();
                    int refGroupPos = type.lastIndexOf("(");
                    type = type.substring(0, refGroupPos).trim();

                    SDisciplineGroup group = new SDisciplineGroup(null, type, 0, 0, null, null);
                    group.setDisciplineCodeAndSemester(code, semester);
                    groups.add(group);
                }
            } else {
                SDisciplineGroup group = new SDisciplineGroup(null, null, toInteger(credits), 0, null, null);
                group.setDisciplineCodeAndSemester(code, semester);
                groups.add(group);
            }
        }

        return groups;
    }
}
