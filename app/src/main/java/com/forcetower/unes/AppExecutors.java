package com.forcetower.unes;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;

@Singleton
public class AppExecutors {
    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;
    private final Executor others;

    public AppExecutors(Executor diskIO, Executor networkIO, Executor mainThread, Executor others) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
        this.others = others;
    }

    @Inject
    public AppExecutors() {
        this(
            Executors.newFixedThreadPool(2),
            Executors.newFixedThreadPool(3),
            new MainThreadExecutor(),
            Executors.newFixedThreadPool(3)
        );
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    public Executor others() {
        return others;
    }

    private static class MainThreadExecutor implements Executor {
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}