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

package com.forcetower.uefs.architecture.service.discipline

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.DOWNLOADING
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.INITIAL
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.LOGIN
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.PROCESSING
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.SAVING
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.repository.DisciplineLoaderRepository
import com.forcetower.uefs.service.NotificationCreator
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class DisciplineDetailsLoaderService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_DISCIPLINE_DETAILS_LOADER = 20546

        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, DisciplineDetailsLoaderService::class.java)
            context.startService(intent)
        }
    }

    @Inject
    lateinit var repository: DisciplineLoaderRepository
    private var running = false

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startComponent()
        return Service.START_STICKY
    }

    private fun startComponent() {
        if (!running) {
            running = true
            Timber.d("Started Discipline Load")
            repository.loadDisciplineDetails().observe(this, Observer { onDataUpdate(it) })
        }
    }

    private fun onDataUpdate(callback: DisciplineDetailsCallback?) {
        callback ?: return

        when (callback.status) {
            Status.LOADING -> {
                generateNotification(callback)
            }
            Status.COMPLETED -> {
                stopForeground(true)
                stopSelf()
            }
            else -> Unit
        }
    }

    private fun generateNotification(callback: DisciplineDetailsCallback) {
        val builder = NotificationCreator.disciplineDetailsLoadNotification(this)
        val total = callback.getTotal()
        val current = callback.getCurrent()
        if (callback.getFlags() == DOWNLOADING) {
            builder.setContentText(getString(R.string.step_discipline_details_downloading, current, total))
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(total, current, true)
            val message = when (callback.getFlags()) {
                LOGIN -> getString(R.string.step_discipline_details_login)
                INITIAL -> getString(R.string.step_discipline_details_initial)
                PROCESSING -> getString(R.string.step_discipline_details_processing)
                SAVING -> getString(R.string.step_discipline_details_saving)
                else -> getString(R.string.step_undefined)
            }
            builder.setContentText(message)
        }
        startForeground(NOTIFICATION_DISCIPLINE_DETAILS_LOADER, builder.build())
    }
}