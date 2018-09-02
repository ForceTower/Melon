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