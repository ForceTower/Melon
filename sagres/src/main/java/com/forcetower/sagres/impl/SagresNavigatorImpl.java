package com.forcetower.sagres.impl;

import android.content.Context;

import com.forcetower.sagres.SagresNavigator;
import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.database.model.SagresAccess;
import com.forcetower.sagres.executor.SagresTaskExecutor;
import com.forcetower.sagres.operation.calendar.CalendarCallback;
import com.forcetower.sagres.operation.calendar.CalendarOperation;
import com.forcetower.sagres.operation.login.LoginOperation;
import com.forcetower.sagres.operation.login.LoginCallback;
import com.forcetower.sagres.operation.messages.MessagesCallback;
import com.forcetower.sagres.operation.messages.MessagesOperation;
import com.forcetower.sagres.operation.person.PersonCallback;
import com.forcetower.sagres.operation.person.PersonOperation;
import com.forcetower.sagres.operation.semester.SemesterCallback;
import com.forcetower.sagres.operation.semester.SemesterOperation;
import com.forcetower.sagres.operation.start_page.StartPageCallback;
import com.forcetower.sagres.operation.start_page.StartPageOperation;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SagresNavigatorImpl extends SagresNavigator {
    private OkHttpClient mClient;
    private SagresDatabase mDatabase;

    private static SagresNavigatorImpl sDefaultInstance = null;
    private static final Object sLock = new Object();

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull SagresNavigatorImpl getInstance() {
        synchronized (sLock) {
            return sDefaultInstance;
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void initialize(@NonNull Context context) {
        synchronized (sLock) {
            if (sDefaultInstance == null) {
                context = context.getApplicationContext();
                if (sDefaultInstance == null) {
                    sDefaultInstance = new SagresNavigatorImpl(context);
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private SagresNavigatorImpl(@NonNull Context context) {
        this.mDatabase = SagresDatabase.create(context);
        this.mClient = createClient(context);
    }

    private OkHttpClient createClient(Context context) {
        return new OkHttpClient.Builder()
                .followRedirects(true)
                .cookieJar(createCookieJar(context))
                .addInterceptor(createInterceptor())
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .build();
    }

    private Interceptor createInterceptor() {
        return chain -> {
            SagresAccess access = mDatabase.accessDao().getAccessDirect();
            Request oRequest = chain.request();
            oRequest = oRequest.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36")
                    .build();

            if (access == null) {
                return chain.proceed(oRequest);
            } else {
                String credentials = Credentials.basic(access.getUsername(), access.getPassword());
                if (oRequest.header("Authorization") != null) {
                    return chain.proceed(oRequest);
                }

                Request nRequest = oRequest.newBuilder()
                        .addHeader("Authorization", credentials)
                        .addHeader("Accept", "application/json")
                        .build();

                return chain.proceed(nRequest);
            }
        };
    }

    private PersistentCookieJar createCookieJar(Context context) {
        return new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LiveData<LoginCallback> aLogin(@NonNull String username, @NonNull String password) {
        return new LoginOperation(username, password, SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @Override
    @Nullable
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SagresNavigator login(@NonNull String username, @NonNull String password) {
        boolean successful = new LoginOperation(username, password, null).isSuccessful();
        if (successful) return this;
        return null;
    }

    @NonNull
    @Override
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LiveData<PersonCallback> aMe() {
        return new PersonOperation(null, SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @Override
    @Nullable
    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SagresNavigator me() {
        boolean successful = new PersonOperation(null, null).isSuccess();
        if (successful) return this;
        return null;
    }

    @NonNull
    @Override
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LiveData<MessagesCallback> aMessages(long userId) {
        return new MessagesOperation(SagresTaskExecutor.getNetworkThreadExecutor(), userId).getResult();
    }

    @NonNull
    @Override
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LiveData<CalendarCallback> aCalendar() {
        return new CalendarOperation(SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @NotNull
    @Override
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public LiveData<SemesterCallback> aSemesters(long userId) {
        return new SemesterOperation(userId, SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @NonNull
    @Override
    public LiveData<StartPageCallback> startPage() {
        return new StartPageOperation(SagresTaskExecutor.getNetworkThreadExecutor()).getResult();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public OkHttpClient getClient() {
        return mClient;
    }

    @Override
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public SagresDatabase getDatabase() {
        return mDatabase;
    }
}
