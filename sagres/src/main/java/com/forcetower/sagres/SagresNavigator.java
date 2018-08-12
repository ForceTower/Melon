package com.forcetower.sagres;

import android.content.Context;

import com.forcetower.sagres.database.SagresDatabase;
import com.forcetower.sagres.impl.SagresNavigatorImpl;
import com.forcetower.sagres.operation.calendar.CalendarCallback;
import com.forcetower.sagres.operation.login.LoginCallback;
import com.forcetower.sagres.operation.messages.MessagesCallback;
import com.forcetower.sagres.operation.person.PersonCallback;
import com.forcetower.sagres.operation.start_page.StartPageCallback;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

public abstract class SagresNavigator {
    public static @NonNull SagresNavigator getInstance() {
        return SagresNavigatorImpl.getInstance();
    }

    public static void initialize(@NonNull Context context) {
        SagresNavigatorImpl.initialize(context);
    }

    @NonNull
    @AnyThread
    public abstract LiveData<LoginCallback> aLogin(@NonNull String username, @NonNull String password);

    @Nullable
    @WorkerThread
    public abstract SagresNavigator login(@NonNull String username, @NonNull String password);

    @NonNull
    @AnyThread
    public abstract LiveData<PersonCallback> aMe();

    @Nullable
    @WorkerThread
    public abstract SagresNavigator me();

    @NonNull
    @AnyThread
    public abstract LiveData<MessagesCallback> aMessages(long userId);

    @NonNull
    @AnyThread
    public abstract LiveData<CalendarCallback> aCalendar();

    @NonNull
    @AnyThread
    public abstract LiveData<StartPageCallback> startPage();

    @NonNull
    public abstract SagresDatabase getDatabase();

    public abstract void stopTags(@NonNull String tag);
}
