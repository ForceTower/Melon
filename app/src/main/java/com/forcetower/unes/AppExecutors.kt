package com.forcetower.unes

import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.Executors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppExecutors(private val diskIO: Executor, private val networkIO: Executor, private val mainThread: Executor, private val others: Executor) {

    @Inject
    constructor() : this(
            Executors.newFixedThreadPool(2),
            Executors.newFixedThreadPool(4),
            MainThreadExecutor(),
            Executors.newFixedThreadPool(3)
    )

    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    fun others(): Executor {
        return others
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}