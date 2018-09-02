/*
 * Copyright (c) 2018.
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
