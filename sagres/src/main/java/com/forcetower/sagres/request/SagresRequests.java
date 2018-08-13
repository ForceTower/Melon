package com.forcetower.sagres.request;

import com.forcetower.sagres.Constants;
import com.forcetower.sagres.database.model.Linker;

import org.jetbrains.annotations.NotNull;

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

    public static Request link(Linker linker) {
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
}
