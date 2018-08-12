package com.forcetower.sagres.request;

import com.forcetower.sagres.database.model.Linker;
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

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SagresCalls {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static Call getCall(Request request) {
        OkHttpClient client = SagresNavigatorImpl.getInstance().getClient();
        return client.newCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call login(@NonNull String username, @NonNull String password) {
        RequestBody body = SagresForms.loginBody(username, password);
        Request request = SagresRequests.loginRequest(body);
        return getCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call loginApproval(@NonNull Document document, @NonNull Response response) {
        URL responsePath = response.request().url().url();
        String url = responsePath.getHost() + responsePath.getPath();
        RequestBody body = SagresForms.loginApprovalBody(document);
        Request request = SagresRequests.loginApprovalRequest(url, body);
        return getCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call getMe() {
        Request request = SagresRequests.me();
        return getCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call getPerson(Long userId) {
        Request request = SagresRequests.getPerson(userId);
        return getCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call getLink(Linker linker) {
        Request request = SagresRequests.link(linker);
        return getCall(request);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Call getMessages(long userId) {
        Request request = SagresRequests.messages(userId);
        return getCall(request);
    }

    public static Call getStartPage() {
        Request request = SagresRequests.startPage();
        return getCall(request);
    }
}
