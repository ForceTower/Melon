/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.unes.usecases.version

import android.content.Context
import android.content.SharedPreferences
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.task.UseCase
import com.forcetower.uefs.service.NotificationCreator
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dagger.Reusable
import javax.inject.Inject

@Reusable
class NotifyNewVersionUseCase @Inject constructor(
    private val context: Context,
    private val remoteConfig: FirebaseRemoteConfig,
    private val preferences: SharedPreferences
) : UseCase<Unit, Unit>() {
    override suspend fun execute(parameters: Unit) {
        val currentVersion = remoteConfig.getLong("version_current")
        val notified = preferences.getBoolean("version_ntf_key_$currentVersion", false)
        if (currentVersion > BuildConfig.VERSION_CODE && !notified) {
            val notes = remoteConfig.getString("version_notes")
            val version = remoteConfig.getString("version_name")
            val title = context.getString(R.string.new_version_ntf_title_format, version)
            NotificationCreator.showSimpleNotification(context, title, notes)
            preferences.edit().putBoolean("version_ntf_key_$currentVersion", true).apply()
        }
    }
}
