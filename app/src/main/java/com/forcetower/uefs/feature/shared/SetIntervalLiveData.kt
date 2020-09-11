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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

private const val ONE_SECOND = 1_000L

/**
 * LiveData that applies a map operation to a source at an interval.
 *
 * This is useful if you want to transform a source LiveData on a timer, for example to show
 * a countdown timer to the user based on data provided in source liveData.
 *
 * @param source The source LiveData to transform
 * @param intervalMs How often to run the map operation on the last value provided from source
 * @param map operation to map source data to output
 */
open class SetIntervalLiveData<in P, R>(
    source: LiveData<P>,
    private val intervalMs: Long = ONE_SECOND,
    private val map: (P?) -> R?
) : MediatorLiveData<R>() {

    private var lastEmitted: P? = null

    // Only run the timer if there is an active observer
    private var isActive = false

    // Use the task scheduler to manage threads
    private val taskScheduler = DefaultScheduler

    init {
        addSource(source) { value ->
            lastEmitted = value
            // since this is not in the background thread, go to background before running
            // (potentially expensive) map operation.
            taskScheduler.execute(::updateValue)
        }
    }

    object DefaultIntervalMapper : IntervalMapper {

        private var delegate: IntervalMapper = IntervalMapperDelegate

        fun setDelegate(value: IntervalMapper?) {
            delegate = value ?: IntervalMapperDelegate
        }

        override fun <P, R> mapAtInterval(
            source: LiveData<P>,
            interval: Long,
            map: (P?) -> R?
        ): SetIntervalLiveData<P, R> {
            return delegate.mapAtInterval(source, interval, map)
        }
    }

    internal object IntervalMapperDelegate : IntervalMapper {
        override fun <P, R> mapAtInterval(
            source: LiveData<P>,
            interval: Long,
            map: (P?) -> R?
        ): SetIntervalLiveData<P, R> {
            return SetIntervalLiveData(source, interval, map)
        }
    }

    interface IntervalMapper {

        /**
         * Apply a map operation to a LiveData repeatedly on an interval.
         *
         * @param source LiveData to transform (the source)
         * @param interval how often to run the transform
         * @param map operation to map the source data to output
         */
        fun <P, R> mapAtInterval(
            source: LiveData<P>,
            interval: Long = ONE_SECOND,
            map: (P?) -> R?
        ): SetIntervalLiveData<P, R>
    }

    /**
     * In a background thread, run the map operation (which is potentially expensive), then post
     * the result to any observers.
     */
    @WorkerThread
    fun updateValue() {
        postValue(map(lastEmitted))
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
