package com.forcetower.sagres.parsers;

import com.forcetower.sagres.database.model.Discipline;
import com.forcetower.sagres.utils.ValueUtils;
import com.forcetower.sagres.utils.WordUtils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SagresDisciplineParser {

    public static List<Discipline> getDisciplines(Document document) {
        List<Discipline> disciplines = new ArrayList<>();

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
            String name = title.substring(codePos + 1);

            Discipline discipline = new Discipline(period, name, code);
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
