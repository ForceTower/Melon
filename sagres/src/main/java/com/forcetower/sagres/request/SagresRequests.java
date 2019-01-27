/*
 * Copyright (c) 2019.
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

package com.forcetower.sagres.request;

import androidx.annotation.NonNull;
import com.forcetower.sagres.Constants;
import com.forcetower.sagres.database.model.SLinker;

import androidx.annotation.Nullable;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

    public static Request link(String href) {
        String url = BASE_URL + (href.startsWith("/") ? href : "/" + href);
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
    public static Request getGradesForSemester(long semester, @NonNull Document document, @Nullable Long variant) {
        FormBody.Builder formBody = new FormBody.Builder();

        Elements elements = document.select("input[value][type=\"hidden\"]");

        for (Element element : elements) {
            String key = element.attr("id");
            String value = element.attr("value");
            formBody.add(key, value);
        }

        formBody.add("ctl00$MasterPlaceHolder$ddPeriodosLetivos$ddPeriodosLetivos", Long.valueOf(semester).toString());
        if (variant != null) {
            formBody.add("ctl00$MasterPlaceHolder$ddRegistroCurso", variant.toString());
        }
        formBody.add("ctl00$MasterPlaceHolder$imRecuperar", "Exibir");
        return new Request.Builder()
                .url(Constants.SAGRES_GRADE_ANY)
                .post(formBody.build())
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NonNull
    public static Request getPageRequest(@NotNull String url) {
        return new Request.Builder()
                .addHeader("Accept", "*/*")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.92 Safari/537.36")
                .url(url)
                .build();
    }

    @NotNull
    public static Request postAtStudentPage(@NotNull FormBody.Builder builder) {
        return new Request.Builder()
                .url(Constants.SAGRES_DIARY_PAGE)
                .post(builder.build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request getDisciplinePageWithParams(@NotNull FormBody.Builder params) {
        return new Request.Builder()
                .url(Constants.SAGRES_CLASS_PAGE)
                .post(params.build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request getDemandPage() {
        return new Request.Builder()
                .url(Constants.SAGRES_DEMAND_OFFERS)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request createDemandWithParams(@NotNull RequestBody body) {
        return new Request.Builder()
                .url(Constants.SAGRES_DEMAND_OFFERS)
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .build();
    }

    @NotNull
    public static Request getRequestedServices() {
        return new Request.Builder()
                .url(Constants.SAGRES_REQUESTED_SERVICES)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    @NotNull
    public static Request getMessagesPage() {
        return new Request.Builder()
                .url(Constants.SAGRES_MESSAGES_PAGE)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }
}
