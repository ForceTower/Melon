package com.forcetower.sagres.operation;

import com.forcetower.sagres.parsers.SagresBasicParser;
import com.forcetower.sagres.request.SagresCalls;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.MediatorLiveData;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static com.forcetower.sagres.Utils.createDocument;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LoginOperation {
    @NonNull private final MediatorLiveData<LoginCallback> result;
    @NonNull private final String username;
    @NonNull private final String password;
    @Nullable private final Executor executor;

    private boolean success;

    @AnyThread
    public LoginOperation(@NonNull String username, @NonNull String password, @Nullable Executor executor) {
        this.username = username;
        this.password = password;
        this.executor = executor;
        this.result = new MediatorLiveData<>();
        this.success = false;
        this.perform();
    }

    private void perform() {
        if (executor != null) {
            Timber.d("Executing on Executor");
            executor.execute(this::execute);
        }
        else {
            Timber.d("Executing on Current Thread");
            this.execute();
        }
    }

    private void execute() {
        result.postValue(LoginCallback.started());
        Call call = SagresCalls.login(username, password);

        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                String string = body.string();
                resolveLogin(string, response);
            } else {
                result.postValue(new LoginCallback.Builder(Status.RESPONSE_FAILED).code(response.code()).build());
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build());
        }
    }

    private void resolveLogin(@NonNull String string, @NonNull Response response) {
        Document document = createDocument(string);
        if (SagresBasicParser.isConnected(document)) {
            if (SagresBasicParser.needApproval(document)) {
                result.postValue(new LoginCallback.Builder(Status.LOADING).message("Need approval").build());
                approval(document, response);
            } else {
                success = true;
                result.postValue(new LoginCallback.Builder(Status.SUCCESS).build());
            }
        } else {
            result.postValue(new LoginCallback.Builder(Status.INVALID_LOGIN).build());
        }
    }

    private void approval(@NonNull Document document, @NonNull Response oldResp) {
        Call call = SagresCalls.loginApproval(document, oldResp);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                success = true;
                result.postValue(new LoginCallback.Builder(Status.SUCCESS).build());
            } else {
                result.postValue(new LoginCallback.Builder(Status.APPROVAL_ERROR).code(response.code()).build());
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build());
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSuccessful() {
        return success;
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public MediatorLiveData<LoginCallback> getResult() {
        return result;
    }
}
