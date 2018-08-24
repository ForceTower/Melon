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

package com.forcetower.sagres.request;

import com.forcetower.sagres.Constants;
import com.forcetower.sagres.database.model.SLinker;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import androidx.annotation.NonNull;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SagresRequests {
    public static final String BASE_URL = "http://academico2.uefs.br/Api/SagresApi";

    public static Request loginRequest(RequestBody body) {
        return new Request.Builder()
                .url(Constants.SAGRES_LOGIN_PAGE)
                .tag("aLogin")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    public static Request loginApprovalRequest(String url, RequestBody body) {
        return new Request.Builder()
                .url("http://" + url)
                .post(body)
                .tag("aLogin")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    public static Request me() {
        return new Request.Builder()
                .url("http://academico2.uefs.br/Api/SagresApi/eu")
                .build();
    }

    public static Request link(SLinker linker) {
        String link = linker.getLink();
        String url = BASE_URL + (link.startsWith("/") ? link : "/" + link);
        return new Request.Builder().url(url).build();
    }

    public static Request getPerson(long userId) {
        String url = BASE_URL + "/registro/pessoas/" + userId;
        return new Request.Builder().url(url).build();
    }

    public static Request messages(long userId) {
        String url = BASE_URL + "/diario/recados?idPessoa=" + Long.toString(userId);
        return new Request.Builder()
                .url(url)
                .build();
    }

    public static Request startPage() {
        return new Request.Builder()
                .url(Constants.SAGRES_DIARY_PAGE)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request getSemesters(long userId) {
        String url = BASE_URL + "/diario/periodos-letivos?idPessoa=" + userId + "&perfil=1";
        return new Request.Builder()
                .url(url)
                .build();
    }

    @NotNull
    public static Request getCurrentGrades() {
        return new Request.Builder()
                .url(Constants.SAGRES_GRADE_PAGE)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request getGradesForSemester(long semester, @NonNull Document document) {
        FormBody.Builder formBody = new FormBody.Builder();

        Elements elements = document.select("input[value][type=\"hidden\"]");

        for (Element element : elements) {
            String key = element.attr("id");
            String value = element.attr("value");
            formBody.add(key, value);
        }

        formBody.add("ctl00$MasterPlaceHolder$ddPeriodosLetivos$ddPeriodosLetivos", Long.valueOf(semester).toString());
        formBody.add("ctl00$MasterPlaceHolder$imRecuperar", "Exibir");
        return new Request.Builder()
                .url(Constants.SAGRES_GRADE_ANY)
                .post(formBody.build())
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }
}
