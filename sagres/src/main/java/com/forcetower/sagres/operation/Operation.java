package com.forcetower.sagres.operation;

import com.google.gson.Gson;

import java.util.concurrent.Executor;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MediatorLiveData;
import timber.log.Timber;

public abstract class Operation<Result extends BaseCallback> {
    @NonNull protected final Gson gson;
    @NonNull protected final MediatorLiveData<Result> result;
    @Nullable protected final Executor executor;
    @Nullable protected Result finished;
    protected boolean success;

    @AnyThread
    public Operation(@Nullable Executor executor) {
        this.result = new MediatorLiveData<>();
        this.executor = executor;
        gson = new Gson();
    }

    protected final void perform() {
        if (executor != null) {
            Timber.d("Executing on Executor");
            executor.execute(this::execute);
        }
        else {
            Timber.d("Executing on Current Thread");
            this.execute();
        }
    }

    protected abstract void execute();

    @NonNull
    public MediatorLiveData<Result> getResult() {
        return result;
    }

    @Nullable
    public Result getFinishedResult() {
        return finished;
    }

    public boolean isSuccess() {
        return success;
    }
}
