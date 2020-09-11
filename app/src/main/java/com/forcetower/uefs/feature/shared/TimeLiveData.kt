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

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData

class TimeLiveData<T>(
    private val intervalMs: Long = 1_000L,
    @UiThread private val function: () -> T
) : MutableLiveData<T>() {
    private var isActive = false
    private val taskScheduler = DefaultScheduler

    init {
        taskScheduler.execute(::updateValue)
    }

    /**
     * In a background thread, run the map operation (which is potentially expensive), then post
     * the result to any observers.
     */
    @WorkerThread
    fun updateValue() {
        postValue(function())
    }

    /**
     * In the foreground thread, re-schedule itself on an interval. Each tick, call [updateValue]
     *
     * This will not execute when there are no observers.
     */
    @UiThread
    private fun onInterval() {
        if (!isActive) {
            return // don't process if no one is listening
        }
        taskScheduler.execute(::updateValue)
        taskScheduler.postDelayedToMainThread(intervalMs, ::onInterval)
    }

    /**
     * Called when at an observer is watching this LiveData. It will only be called once for the
     * transition between 0 and 1 observers.
     *
     * Start the interval.
     */
    override fun onActive() {
        super.onActive()
        isActive = true
        taskScheduler.postDelayedToMainThread(intervalMs, ::onInterval)
    }

    /**
     * Called when there are zero observers.
     *
     * Stop the interval.
     */
    override fun onInactive() {
        super.onInactive()
        isActive = false
    }
}
