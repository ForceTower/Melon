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

import com.forcetower.sagres.database.model.SDiscipline;
import com.forcetower.sagres.utils.ValueUtils;
import com.forcetower.sagres.utils.WordUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SagresDisciplineParser {

    public static List<SDiscipline> getDisciplines(Document document) {
        List<SDiscipline> disciplines = new ArrayList<>();

        Elements elements = document.select("section[class=\"webpart-aluno-item\"]");
        for (Element dElement : elements) {
            String title      = dElement.selectFirst("a[class=\"webpart-aluno-nome cor-destaque\"]").text();
            String period     = dElement.selectFirst("span[class=\"webpart-aluno-periodo\"]").text();
            String credits    = dElement.select("span[class=\"webpart-aluno-codigo\"]").text();
            credits = credits.replaceAll("[^\\d]", "");

            Element studentLinks    = dElement.selectFirst("div[class=\"webpart-aluno-links webpart-aluno-links-up\"]");
            if (studentLinks == null)
                studentLinks = dElement.selectFirst("div[class=\"webpart-aluno-links webpart-aluno-links-down\"]");
            Element misses          = studentLinks.child(1);
            Element missesSpan      = misses.selectFirst("span");
            String missedClasses    = missesSpan.text();
            missedClasses = missedClasses.replaceAll("[^\\d]", "");

            String situation = null;
            Element situationPart = dElement.selectFirst("div[class=\"webpart-aluno-resultado\"]");
            if (situationPart == null) situationPart = dElement.selectFirst("div[class=\"webpart-aluno-resultado estado-sim\"]");
            if (situationPart == null) situationPart = dElement.selectFirst("div[class=\"webpart-aluno-resultado estado-nao\"]");
            if (situationPart != null && situationPart.children().size() == 2) {
                situation = situationPart.children().get(1).text();
                situation = situation.toLowerCase();
                situation = WordUtils.toTitleCase(situation);
                if (situation.equalsIgnoreCase("Não existe resultado final divulgado pelo professor."))
                    situation = "Em aberto";
            }

            String last = "";
            String next = "";
            Elements lastAndNextClasses = dElement.select("div[class=\"webpart-aluno-detalhe\"]");
            if (lastAndNextClasses.size() > 0) {
                Element lastSpan = lastAndNextClasses.get(0).selectFirst("span");
                last = lastSpan.text();
            }

            if (lastAndNextClasses.size() > 1) {
                Element nextSpan = lastAndNextClasses.get(1).selectFirst("span");
                next = nextSpan.text();
            }

            int codePos = title.indexOf("-");
            String code = title.substring(0, codePos).trim();
            String name = title.substring(codePos + 1).trim();

            SDiscipline discipline = new SDiscipline(period, name, code);
            discipline.setCredits(ValueUtils.toInteger(credits));
            discipline.setMissedClasses(ValueUtils.toInteger(missedClasses));
            discipline.setLastClass(last);
            discipline.setNextClass(next);
            discipline.setSituation(situation);
            disciplines.add(discipline);
        }

        return disciplines;

    }
}
