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

package com.forcetower.sagres.executor;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultTaskExecutor extends TaskExecutor {

    private final Object mLock = new Object();

    private final ExecutorService mDiskIO = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private static final String THREAD_NAME_STEM = "unes_disk_io_%d";

        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format(THREAD_NAME_STEM, mThreadId.getAndIncrement()));
            return t;
        }
    });

    private final ExecutorService mNetworkIO = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private static final String THREAD_NAME_STEM = "unes_net_io_%d";

        private final AtomicInteger mThreadId = new AtomicInteger(0);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format(THREAD_NAME_STEM, mThreadId.getAndIncrement()));
            return t;
        }
    });

    @Nullable
    private volatile Handler mMainHandler;

    @Override
    public void executeOnDiskIO(@NonNull Runnable runnable) {
        mDiskIO.execute(runnable);
    }

    @Override
    public void executeOnNetworkIO(@NonNull Runnable runnable) {
        mNetworkIO.execute(runnable);
    }

    @Override
    public void postToMainThread(@NonNull Runnable runnable) {
        if (mMainHandler == null) {
            synchronized (mLock) {
                if (mMainHandler == null) {
                    mMainHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        //noinspection ConstantConditions
        mMainHandler.post(runnable);
    }

    @Override
    public boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}
