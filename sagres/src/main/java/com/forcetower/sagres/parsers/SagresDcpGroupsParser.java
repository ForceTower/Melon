package com.forcetower.sagres.parsers;

import com.forcetower.sagres.database.model.DisciplineGroup;

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

    public static List<DisciplineGroup> getGroups(Document document) {
        List<DisciplineGroup> groups = new ArrayList<>();

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

                    DisciplineGroup group = new DisciplineGroup(0, null, type, 0, 0, null, null);
                    group.setDisciplineCodeAndSemester(code, semester);
                    groups.add(group);
                }
            } else {
                DisciplineGroup group = new DisciplineGroup(0, null, null, toInteger(credits), 0, null, null);
                group.setDisciplineCodeAndSemester(code, semester);
                groups.add(group);
            }
        }

        return groups;
    }
}
