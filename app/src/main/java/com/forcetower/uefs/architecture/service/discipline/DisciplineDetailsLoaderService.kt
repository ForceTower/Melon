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

package com.forcetower.uefs.architecture.service.discipline

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleService
import com.forcetower.sagres.operation.Status
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.DOWNLOADING
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.GRADES
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.INITIAL
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.LOGIN
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.PROCESSING
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback.Companion.SAVING
import com.forcetower.sagres.operation.disciplines.FastDisciplinesCallback
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.repository.DisciplineDetailsRepository
import com.forcetower.uefs.core.util.isConnectedToInternet
import com.forcetower.uefs.service.NotificationCreator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class DisciplineDetailsLoaderService : LifecycleService() {
    companion object {
        private const val NOTIFICATION_DISCIPLINE_DETAILS_LOADER = 20546
        const val EXTRA_SHOW_CONTRIBUTING_NOTIFICATION = "show_contributing_notification"

        @JvmStatic
        fun startService(context: Context, contributing: Boolean = false) {
            val intent = Intent(context, DisciplineDetailsLoaderService::class.java).apply {
                putExtra(EXTRA_SHOW_CONTRIBUTING_NOTIFICATION, contributing)
            }
            context.startService(intent)
        }
    }

    @Inject
    lateinit var repository: DisciplineDetailsRepository
    @Inject
    lateinit var preferences: SharedPreferences

    private var running = false
    private var contributing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val value = intent?.getBooleanExtra(EXTRA_SHOW_CONTRIBUTING_NOTIFICATION, false) ?: false
        contributing = value || contributing

        startComponent()
        return Service.START_STICKY
    }

    private fun startComponent() {
        if (!running) {
            running = true
            Timber.d("Started Discipline Load")
            repository.loadDisciplineDetails(partialLoad = contributing, discover = false).observe(this, { onDataUpdate(it) })
        }
    }

    private fun onDataUpdate(callback: FastDisciplinesCallback?) {
        callback ?: return

        when (callback.status) {
            Status.LOADING -> {
                generateNotification(callback)
            }
            Status.COMPLETED -> {
                if (isConnectedToInternet() && callback.getFailureCount() <= 5) {
                    val current = preferences.getInt("hourglass_status", 0)
                    if (current < 1) preferences.edit().putInt("hourglass_status", 1).apply()
                    if (contributing) NotificationCreator.createCompletedDisciplineLoadNotification(this)
                } else {
                    finishShowingError()
                }
                // TODO This is a test call and will be removed when it reaches release status
                repository.contribute()
                stopForeground(true)
                stopSelf()
            }
            Status.APPROVING -> Unit
            Status.STARTED -> Unit
            Status.SUCCESS -> Unit
            else -> finishShowingError()
        }
    }

    private fun finishShowingError() {
        val title = getString(R.string.failed_to_download_disciplines)
        val message = getString(R.string.failed_to_download_disciplines_message)
        NotificationCreator.createFailedWarningNotification(this, title, message)
        stopForeground(true)
        stopSelf()
    }

    private fun generateNotification(callback: FastDisciplinesCallback) {
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
                GRADES -> getString(R.string.step_discipline_details_grades)
                else -> getString(R.string.step_undefined)
            }
            builder.setContentText(message)
        }
        startForeground(NOTIFICATION_DISCIPLINE_DETAILS_LOADER, builder.build())
    }
}
