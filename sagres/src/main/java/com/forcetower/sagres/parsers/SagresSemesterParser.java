package com.forcetower.sagres.parsers;

import com.forcetower.sagres.database.model.Semester;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class SagresSemesterParser {

    public static List<Semester> getSemesters(Document document) {
        List<Semester> semesters = new ArrayList<>();
        Elements classes = document.select("section[class=\"webpart-aluno-item\"]");

        List<String> strings = new ArrayList<>();
        for (Element element : classes) {
            String period = element.selectFirst("span[class=\"webpart-aluno-periodo\"]").text();
            period = period.toLowerCase();
            if (!strings.contains(period)) strings.add(period);
        }
        Timber.d("Semesters: %s", strings);
        for (int i = 0; i < strings.size(); i++) {
            semesters.add(new Semester(strings.size() - i, strings.get(i), strings.get(i), "", "", "", ""));
        }

        return semesters;
    }
}