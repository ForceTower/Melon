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

package com.forcetower.uefs

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.storage.cookies.PrefsCookiePersistor
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.forcetower.uefs.feature.themeswitcher.ThemePreferencesManager
import com.forcetower.uefs.impl.AndroidBase64Encoder
import com.forcetower.uefs.impl.CrashlyticsTree
import com.forcetower.uefs.impl.SharedPrefsCachePersistence
import com.forcetower.uefs.service.NotificationHelper
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class UApplication : Application(), Configuration.Provider {
    @Inject lateinit var preferences: SharedPreferences
    @Inject lateinit var workerFactory: HiltWorkerFactory

    var disciplineToolbarDevClickCount = 0
    var messageToolbarDevClickCount = 0

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
        super.onCreate()

        setupDayNightTheme(this)
        defineWorker()
    }

    private fun defineWorker() {
        val worker = preferences.getString("stg_sync_worker_type", "0")?.toIntOrNull() ?: 0
        val period = preferences.getString("stg_sync_frequency", "60")?.toIntOrNull() ?: 60
        when (worker) {
            0 -> SyncMainWorker.createWorker(this, period)
            1 -> Unit // SyncLinkedWorker.createWorker(period, false)
        }
    }

    /**
     * Inicializa o objeto de conexão com o Sagres
     */
    @Inject
    fun configureSagresNavigator(client: OkHttpClient) {
        val selected = preferences.getString(Constants.SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"
        SagresNavigator.initialize(
            PrefsCookiePersistor(this),
            selected,
            AndroidBase64Encoder(),
            SharedPrefsCachePersistence(preferences),
            baseClient = client
        )
    }

    /**
     * Cria/Apaga os canais de notificação
     */
    @Inject
    fun configureNotifications() {
        NotificationHelper(this).createChannels()
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        // Resetting the theme at every theme change is not a good solution
        // Find out a migration path for theme overlays to use in the future
        fun setupDayNightTheme(context: Context, resetTheme: Boolean = false) {
            ThemePreferencesManager(context).run {
                applyTheme()
                if (resetTheme) deleteSavedTheme()
                retrieveOverlay()
            }
        }
    }
}
