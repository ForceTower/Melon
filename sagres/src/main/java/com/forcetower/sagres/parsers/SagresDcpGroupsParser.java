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

                    SDisciplineGroup group = new SDisciplineGroup(null, type, 0, 0, null, null, null);
                    group.setDisciplineCodeAndSemester(code, semester);
                    groups.add(group);
                }
            } else {
                SDisciplineGroup group = new SDisciplineGroup(null, null, toInteger(credits), 0, null, null, null);
                group.setDisciplineCodeAndSemester(code, semester);
                groups.add(group);
            }
        }

        return groups;
    }
}
