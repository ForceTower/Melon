package com.forcetower.sagres.impl;

import android.content.Context;

import com.forcetower.sagres.SagresNavigator;
import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.executor.SagresTaskExecutor;
import com.forcetower.sagres.operation.LoginOperation;
import com.forcetower.sagres.operation.LoginCallback;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import okhttp3.Call;
import okhttp3.OkHttpClient;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SagresNavigatorImpl extends SagresNavigator {
    private OkHttpClient mClient;
    private SagresDatabase mDatabase;

    private static SagresNavigatorImpl sDefaultInstance = null;
    private static final Object sLock = new Object();

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static @Nullable SagresNavigatorImpl getInstance() {
        synchronized (sLock) {
            if (sDefaultInstance != null) {
                return sDefaultInstance;
            }
            return null;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void initialize(@NonNull Context context, @NonNull OkHttpClient client) {
        synchronized (sLock) {
            if (sDefaultInstance == null) {
                context = context.getApplicationContext();
                if (sDefaultInstance == null) {
                    sDefaultInstance = new SagresNavigatorImpl(context, client);
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    private SagresNavigatorImpl(@NonNull Context context, @NonNull OkHttpClient client) {
        this.mDatabase = SagresDatabase.create(context);
        this.mClient = client;
    }

    public void stopTags(@NonNull String tags) {
        List<Call> callList = new ArrayList<>();
        callList.addAll(mClient.dispatcher().runningCalls());
        callList.addAll(mClient.dispatcher().queuedCalls());
        for (Call call : callList) {
            Object tag = call.request().tag();
            if (tag != null && tag.equals(tags)) {
                call.cancel();
            }
        }
    }

    @Override
    @NonNull
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public LiveData<LoginCallback> login(@NonNull String username, @NonNull String password) {
        return new LoginOperation(username, password, SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @Override
    @Nullable
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SagresNavigator loginWith(@NonNull String username, @NonNull String password) {
        boolean successful = new LoginOperation(username, password, null).isSuccessful();
        if (successful) return this;
        return null;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public OkHttpClient getClient() {
        return mClient;
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SagresDatabase getDatabase() {
        return mDatabase;
    }
}
