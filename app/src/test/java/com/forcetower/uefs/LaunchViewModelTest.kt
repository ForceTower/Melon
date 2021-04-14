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

package com.forcetower.uefs

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.forcetower.core.lifecycle.Event
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.task.UCaseResult
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dev.forcetower.unes.usecases.auth.HasEnrolledAccessUseCase
import dev.forcetower.unes.usecases.version.NotifyNewVersionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LaunchViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `move to login when there is no access`() {
        val enrolledUseCase = mockk<HasEnrolledAccessUseCase>()
        val eventsLiveDataObserver = mockk<EventObserver<LaunchViewModel.Destination>>(relaxed = true)

        val viewModel = LaunchViewModel(enrolledUseCase, mockk())

        coEvery { enrolledUseCase(Unit) } returns UCaseResult.Success(false)

        viewModel.findStarterDirection()
        viewModel.direction.observeForever(eventsLiveDataObserver)

        verify {
            eventsLiveDataObserver.onChanged(Event(LaunchViewModel.Destination.LOGIN_ACTIVITY))
        }
    }

    @Test
    fun `move to home when there is a enrolled access`() {
        val enrolledUseCase = mockk<HasEnrolledAccessUseCase>()
        val eventsLiveDataObserver = mockk<EventObserver<LaunchViewModel.Destination>>(relaxed = true)

        val viewModel = LaunchViewModel(enrolledUseCase, mockk())

        coEvery { enrolledUseCase(Unit) } returns UCaseResult.Success(true)
        viewModel.findStarterDirection()
        viewModel.direction.observeForever(eventsLiveDataObserver)

        verify {
            eventsLiveDataObserver.onChanged(Event(LaunchViewModel.Destination.HOME_ACTIVITY))
        }
    }

    @Test
    fun `verifies new version on app`() {
        val preferences = mockk<SharedPreferences>()
        val remoteConfig = mockk<FirebaseRemoteConfig>()
        val notifyCase = NotifyNewVersionUseCase(mockk(), remoteConfig, preferences)

        val versionCode = BuildConfig.VERSION_CODE + 1L
        every { remoteConfig.getLong("version_current") } returns versionCode
        every { preferences.getBoolean("version_ntf_key_$versionCode", false) } returns false

        val viewModel = LaunchViewModel(mockk(), notifyCase)
        viewModel.checkNewAppVersion()

        verify {
            // called to fetch version notes
            remoteConfig.getString("version_notes")
        }
    }

    @Test
    fun `don't show notification if already shown new version on app`() {
        val preferences = mockk<SharedPreferences>()
        val remoteConfig = mockk<FirebaseRemoteConfig>()
        val notifyCase = NotifyNewVersionUseCase(mockk(), remoteConfig, preferences)

        val versionCode = BuildConfig.VERSION_CODE + 1L
        every { remoteConfig.getLong("version_current") } returns versionCode
        every { preferences.getBoolean("version_ntf_key_$versionCode", false) } returns true

        val viewModel = LaunchViewModel(mockk(), notifyCase)
        viewModel.checkNewAppVersion()

        verify(exactly = 0) {
            // never called to fetch version notes
            remoteConfig.getString("version_notes")
        }
    }
}
