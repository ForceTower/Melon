package com.forcetower.sagres.operation.login;

import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.database.model.SagresAccess;
import com.forcetower.sagres.impl.SagresNavigatorImpl;
import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;
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
public class LoginOperation extends Operation<LoginCallback> {
    @NonNull private final String username;
    @NonNull private final String password;

    @AnyThread
    public LoginOperation(@NonNull String username, @NonNull String password, @Nullable Executor executor) {
        super(executor);
        this.username = username;
        this.password = password;
        this.perform();
    }

    protected void execute() {
        result.postValue(LoginCallback.started());
        Call call = SagresCalls.INSTANCE.login(username, password);

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
        if (SagresBasicParser.INSTANCE.isConnected(document)) {
            if (SagresBasicParser.INSTANCE.needApproval(document)) {
                result.postValue(new LoginCallback.Builder(Status.LOADING).message("Need approval").build());
                approval(document, response);
            } else {
                successMeasures(document);
            }
        } else {
            result.postValue(new LoginCallback.Builder(Status.INVALID_LOGIN).build());
        }
    }

    private void approval(@NonNull Document document, @NonNull Response oldResp) {
        Call call = SagresCalls.INSTANCE.loginApproval(document, oldResp);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                successMeasures(document);
            } else {
                result.postValue(new LoginCallback.Builder(Status.APPROVAL_ERROR).code(response.code()).build());
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build());
        }
    }

    private void successMeasures(@NonNull Document document) {
        success = true;

        SagresDatabase database = SagresNavigatorImpl.getInstance().getDatabase();
        SagresAccess access = database.accessDao().getAccessDirect();
        SagresAccess created = new SagresAccess(username, password);
        if (access == null || !access.equals(created)) database.accessDao().insert(created);

        finished = new LoginCallback.Builder(Status.SUCCESS).document(document).build();
        result.postValue(finished);
    }

    @Nullable
    public LoginCallback getFinishedCallback() {
        return finished;
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
