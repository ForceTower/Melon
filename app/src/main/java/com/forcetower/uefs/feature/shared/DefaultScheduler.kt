/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.shared

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val NUMBER_OF_THREADS = 4 // TODO: Make this depend on device's hw

interface Scheduler {

    fun execute(task: () -> Unit)

    fun postToMainThread(task: () -> Unit)

    fun postDelayedToMainThread(delay: Long, task: () -> Unit)
}

/**
 * A shim [Scheduler] that by default handles operations in the [AsyncScheduler].
 */
object DefaultScheduler : Scheduler {

    private var delegate: Scheduler = AsyncScheduler

    /**
     * Sets the new delegate scheduler, null to revert to the default async one.
     */
    fun setDelegate(newDelegate: Scheduler?) {
        delegate = newDelegate ?: AsyncScheduler
    }

    override fun execute(task: () -> Unit) {
        delegate.execute(task)
    }

    override fun postToMainThread(task: () -> Unit) {
        delegate.postToMainThread(task)
    }

    override fun postDelayedToMainThread(delay: Long, task: () -> Unit) {
        delegate.postDelayedToMainThread(delay, task)
    }
}

/**
 * Runs tasks in a [ExecutorService] with a fixed thread of pools
 */
internal object AsyncScheduler : Scheduler {

    private val executorService: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)

    override fun execute(task: () -> Unit) {
        executorService.execute(task)
    }

    override fun postToMainThread(task: () -> Unit) {
        if (isMainThread()) {
            task()
        } else {
            val mainThreadHandler = Handler(Looper.getMainLooper())
            mainThreadHandler.post(task)
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread === Thread.currentThread()
    }

    override fun postDelayedToMainThread(delay: Long, task: () -> Unit) {
        val mainThreadHandler = Handler(Looper.getMainLooper())
        mainThreadHandler.postDelayed(task, delay)
    }
}

/**
 * Runs tasks synchronously.
 */
object SyncScheduler : Scheduler {
    private val postDelayedTasks = mutableListOf<() -> Unit>()

    override fun execute(task: () -> Unit) {
        task()
    }

    override fun postToMainThread(task: () -> Unit) {
        task()
    }

    override fun postDelayedToMainThread(delay: Long, task: () -> Unit) {
        postDelayedTasks.add(task)
    }

    fun runAllScheduledPostDelayedTasks() {
        val tasks = postDelayedTasks.toList()
        clearScheduledPostdelayedTasks()
        for (task in tasks) {
            task()
        }
    }

    private fun clearScheduledPostdelayedTasks() {
        postDelayedTasks.clear()
    }
}
