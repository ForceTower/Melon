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
