package com.forcetower.sagres.request;

import com.forcetower.sagres.Constants;

import okhttp3.Request;
import okhttp3.RequestBody;

public class SagresRequests {
    public static Request loginRequest(RequestBody body) {
        return new Request.Builder()
                .url(Constants.SAGRES_LOGIN_PAGE)
                .tag("login")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }

    public static Request loginApprovalRequest(String url, RequestBody body) {
        return new Request.Builder()
                .url("http://" + url)
                .post(body)
                .tag("login")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
    }
}
