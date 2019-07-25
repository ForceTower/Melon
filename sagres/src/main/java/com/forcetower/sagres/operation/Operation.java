/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.sagres.operation;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MediatorLiveData;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.util.concurrent.Executor;

public abstract class Operation<Result extends BaseCallback> {
    @NonNull protected final Gson gson;
    @NonNull protected final MediatorLiveData<Result> result;
    @Nullable protected final Executor executor;
    protected Result finished;
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

    @NonNull
    public Result getFinishedResult() {
        return finished;
    }

    public boolean isSuccess() {
        return success;
    }

    public void publishProgress(@NotNull Result value) {
        finished = value;
        result.postValue(value);
    }
}
