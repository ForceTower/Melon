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

package com.forcetower.uefs.architecture.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.forcetower.uefs.core.storage.repository.UpgradeRepository
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnUpgradeReceiver : BroadcastReceiver() {
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var repository: UpgradeRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED != intent.action) return
        repository.onUpgrade()

        val type = preferences.getString("stg_sync_worker_type", "0")?.toIntOrNull() ?: 0
        if (type != 0) {
            var period = preferences.getString("stg_sync_frequency", "60")?.toIntOrNull() ?: 60
            preferences.edit().putString("stg_sync_worker_type", "0").apply()
            SyncLinkedWorker.stopWorker(context)
            if (period < 15) {
                period = 15
                preferences.edit().putString("stg_sync_frequency", "15").apply()
            }
            SyncMainWorker.createWorker(context, period)
        }
    }
}
