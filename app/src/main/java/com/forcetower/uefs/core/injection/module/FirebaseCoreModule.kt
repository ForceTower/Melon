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

package com.forcetower.uefs.core.injection.module

import android.content.Context
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants.REMOTE_CONFIG_REFRESH
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseCoreModule {
    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Reusable
    fun provideAnalytics(context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    @Reusable
    fun provideMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Reusable
    fun provideRemoteConfig(): FirebaseRemoteConfig {
        val config = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(REMOTE_CONFIG_REFRESH)
            .setFetchTimeoutInSeconds(REMOTE_CONFIG_REFRESH)
            .build()

        config.setConfigSettingsAsync(settings)
        config.setDefaultsAsync(R.xml.remote_config_defaults)
        config.fetchAndActivate().addOnCompleteListener {
            if (it.isSuccessful) {
                Unit
            } else {
                Timber.d("Failed to init remote config")
            }
        }
        return config
    }

    @Provides
    @Reusable
    fun provideFunctions(): FirebaseFunctions {
        return FirebaseFunctions.getInstance()
    }
}
