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

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.R
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Disponibiliza código especifico para trabalhar com as ferramentas do Google Play Games.
 */
abstract class UGameActivity : UActivity() {
    @Inject
    lateinit var mGamesInstance: GooglePlayGamesInstance

    private val showAchievements = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Timber.d("Received result from show achievements")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGamesInstance.signInClient.isAuthenticated().addOnCompleteListener { task ->
            val isAuthenticated = task.isSuccessful && task.result.isAuthenticated
            if (isAuthenticated) {
                onGooglePlayGamesConnected()
            } else {
                mGamesInstance.onDisconnected()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (mGamesInstance.isPlayGamesEnabled()) signInSilently()
        // else checkNotConnectedAchievements()
    }

    private fun signInSilently() {
        val client = mGamesInstance.signInClient
        Timber.d("Signing in!")
        client.signIn()
    }

    fun signIn() {
        PlayGamesSdk.initialize(applicationContext)
        val client = mGamesInstance.signInClient
        Timber.d("Signing in!")
        client.signIn()
    }

    private fun onGooglePlayGamesConnected() {
        mGamesInstance.onConnected()
        checkAchievements()
    }

    open fun checkAchievements() = Unit
    open fun checkNotConnectedAchievements() = Unit

    suspend fun isConnectedToPlayGames() = mGamesInstance.isConnected()
    fun unlockAchievement(@StringRes id: Int) = mGamesInstance.unlockAchievement(id)
    fun unlockAchievement(id: String) = mGamesInstance.unlockAchievement(id)
    fun revealAchievement(@StringRes id: Int) = mGamesInstance.revealAchievement(id)
    fun incrementAchievementProgress(@StringRes id: Int, step: Int) = mGamesInstance.incrementAchievement(id, step)
    fun updateAchievementProgress(@StringRes id: Int, value: Int) = mGamesInstance.updateProgress(id, value)
    fun updateAchievementProgress(id: String, value: Int) = mGamesInstance.updateProgress(id, value)
    fun signOut() = mGamesInstance.disconnect()

    suspend fun openAchievements() {
        if (!mGamesInstance.isConnected()) {
            showSnack(getString(R.string.not_connected_to_the_adventure), Snackbar.LENGTH_LONG)
            return
        }
        val client = mGamesInstance.achievementsClient

        try {
            val intent = client.achievementsIntent.await()
            showAchievements.launch(intent)
        } catch (error: Exception) {
            Timber.e(error, "Device can't open achievements intent")
            showSnack("${getString(R.string.unable_to_open_achievements)} ${error.message}")
        }
    }
}
