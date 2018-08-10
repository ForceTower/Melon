package com.forcetower.sagres.request;

import com.forcetower.sagres.impl.SagresNavigatorImpl;

import org.jsoup.nodes.Document;

import java.net.URL;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SagresCalls {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Call login(@NonNull String username, @NonNull String password) {
        RequestBody body = SagresForms.loginBody(username, password);
        Request request = SagresRequests.loginRequest(body);
        OkHttpClient client = SagresNavigatorImpl.getInstance().getClient();
        return client.newCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Call loginApproval(@NonNull Document document, @NonNull Response response) {
        URL responsePath = response.request().url().url();
        String url = responsePath.getHost() + responsePath.getPath();
        RequestBody body = SagresForms.loginApprovalBody(document);
        Request request = SagresRequests.loginApprovalRequest(url, body);
        OkHttpClient client = SagresNavigatorImpl.getInstance().getClient();
        return client.newCall(request);
    }
}
