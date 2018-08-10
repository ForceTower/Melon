package com.forcetower.sagres;

import android.content.Context;

import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.impl.SagresNavigatorImpl;
import com.forcetower.sagres.operation.LoginCallback;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import okhttp3.OkHttpClient;

public abstract class SagresNavigator {
    public static @Nullable SagresNavigator getInstance() {
        return SagresNavigatorImpl.getInstance();
    }

    public static void initialize(@NonNull Context context, @NonNull OkHttpClient client) {
        SagresNavigatorImpl.initialize(context, client);
    }

    @NonNull
    @AnyThread
    public abstract LiveData<LoginCallback> login(@NonNull String username, @NonNull String password);

    @Nullable
    @WorkerThread
    public abstract SagresNavigator loginWith(@NonNull String username, @NonNull String password);

    public abstract SagresDatabase getDatabase();

    public abstract void stopTags(@NonNull String tag);
}
