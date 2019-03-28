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

package com.forcetower.sagres.operation.login;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.MediatorLiveData;
import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.database.model.SAccess;
import com.forcetower.sagres.impl.SagresNavigatorImpl;
import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;
import com.forcetower.sagres.parsers.SagresBasicParser;
import com.forcetower.sagres.request.SagresCalls;
import com.forcetower.sagres.utils.ConnectedStates;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.nodes.Document;
import timber.log.Timber;

import java.io.IOException;
import java.util.concurrent.Executor;

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
        Call call = SagresCalls.login(username, password);

        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                String string = body.string();
                resolveLogin(string, response);
            } else {
                Document doc = null;
                ResponseBody body = response.body();
                if (body != null) {
                    doc = createDocument(body.string());
                }
                finished = new LoginCallback.Builder(Status.RESPONSE_FAILED).document(doc).code(response.code()).build();
                result.postValue(finished);
            }
        } catch (IOException e) {
            Timber.d("Message: %s", e.getMessage());
            e.printStackTrace();
            finished = new LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build();
            result.postValue(finished);
        }
    }

    private void resolveLogin(@NonNull String string, @NonNull Response response) {
        Document document = createDocument(string);
        ConnectedStates loginState = SagresBasicParser.isConnected(document);

        switch (loginState) {
            case CONNECTED:
                continueWithResolve(document, response);
            case INVALID:
                continueWithInvalidation(document);
            case SESSION_TIMEOUT:
                continueWithStopFlags(document);
            case UNKNOWN:
                continueWithUnknownFlags(document);
        }
    }

    private void continueWithUnknownFlags(Document document) {
        LoginCallback callback = new LoginCallback.Builder(Status.INVALID_LOGIN).code(500).document(document).build();
        publishProgress(callback);
    }

    private void continueWithStopFlags(Document document) {
        LoginCallback callback = new LoginCallback.Builder(Status.INVALID_LOGIN).code(440).document(document).build();
        publishProgress(callback);
    }

    private void continueWithInvalidation(Document document) {
        LoginCallback callback = new LoginCallback.Builder(Status.INVALID_LOGIN).code(401).document(document).build();
        publishProgress(callback);
    }

    private void continueWithResolve(Document document, Response response) {
        if (SagresBasicParser.needApproval(document)) {
            result.postValue(new LoginCallback.Builder(Status.LOADING).message("Need approval").build());
            approval(document, response);
        } else {
            successMeasures(document);
        }
    }

    private void approval(@NonNull Document document, @NonNull Response oldResp) {
        Call call = SagresCalls.loginApproval(document, oldResp);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                document = createDocument(response.body().string());
                successMeasures(document);
            } else {
                finished = new LoginCallback.Builder(Status.APPROVAL_ERROR).code(response.code()).build();
                result.postValue(finished);
            }
        } catch (IOException e) {
            e.printStackTrace();
            finished = new LoginCallback.Builder(Status.NETWORK_ERROR).throwable(e).build();
            result.postValue(finished);
        }
    }

    private void successMeasures(@NonNull Document document) {
        success = true;

        SagresDatabase database = SagresNavigatorImpl.Companion.getInstance().getDatabase();
        SAccess access = database.accessDao().getAccessDirect();
        SAccess created = new SAccess(username, password);
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
