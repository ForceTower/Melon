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

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.PLAY_GAMES_ACHIEVEMENTS
import com.forcetower.uefs.PLAY_GAMES_SIGN_IN
import com.forcetower.uefs.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS
import com.google.android.gms.common.ConnectionResult.NETWORK_ERROR
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import javax.inject.Inject

/**
 * Disponibiliza código especifico para trabalhar com as ferramentas do Google Play Games.
 */
abstract class UGameActivity : UActivity() {
    @Inject
    lateinit var mGamesInstance: GooglePlayGamesInstance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGamesInstance.createGoogleClient()
    }

    override fun onStart() {
        super.onStart()
        if (mGamesInstance.isPlayGamesEnabled()) signInSilently()
        // else checkNotConnectedAchievements()
    }

    private fun signInSilently() {
        mGamesInstance.signInClient?.silentSignIn()?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val result = task.result
                if (result != null) {
                    onGooglePlayGamesConnected(result)
                    return@addOnCompleteListener
                }
            }
            signIn()
        }
    }

    fun signIn() {
        val client = mGamesInstance.signInClient
        client ?: return
        Timber.d("Signing in!")
        startActivityForResult(client.signInIntent, PLAY_GAMES_SIGN_IN)
    }

    private fun onGooglePlayGamesConnected(account: GoogleSignInAccount) {
        mGamesInstance.onConnected(account)
        mGamesInstance.gamesClient?.setViewForPopups(window.decorView.findViewById(android.R.id.content))
        checkAchievements(account.email)
    }

    open fun checkAchievements(email: String?) = Unit
    open fun checkNotConnectedAchievements() = Unit

    fun isConnectedToPlayGames() = mGamesInstance.isConnected()
    fun unlockAchievement(@StringRes id: Int) = mGamesInstance.unlockAchievement(id)
    fun unlockAchievement(id: String) = mGamesInstance.unlockAchievement(id)
    fun revealAchievement(@StringRes id: Int) = mGamesInstance.revealAchievement(id)
    fun incrementAchievementProgress(@StringRes id: Int, step: Int) = mGamesInstance.incrementAchievement(id, step)
    fun updateAchievementProgress(@StringRes id: Int, value: Int) = mGamesInstance.updateProgress(id, value)
    fun updateAchievementProgress(id: String, value: Int) = mGamesInstance.updateProgress(id, value)
    fun signOut() = mGamesInstance.disconnect()

    fun openAchievements() {
        if (!mGamesInstance.isConnected()) {
            showSnack(getString(R.string.not_connected_to_the_adventure), Snackbar.LENGTH_LONG)
            return
        }
        val client = mGamesInstance.achievementsClient
        if (client == null) {
            showSnack(getString(R.string.achievements_client_invalid_reconnect), Snackbar.LENGTH_LONG)
            return
        }

        client.achievementsIntent.addOnCompleteListener {
            try {
                if (it.isSuccessful) {
                    val intent = it.result
                    if (intent != null) {
                        startActivityForResult(intent, PLAY_GAMES_ACHIEVEMENTS)
                        return@addOnCompleteListener
                    } else {
                        showSnack(getString(R.string.cant_open_achievements_invalid_intent), Snackbar.LENGTH_LONG)
                    }
                } else {
                    showSnack("${getString(R.string.unable_to_open_achievements)} ${it.exception?.message}")
                }
            } catch (error: Throwable) {
                Timber.e(error, "Device can't open achievements intent")
                showSnack(getString(R.string.achievements_throwable_unhandled_error))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PLAY_GAMES_SIGN_IN -> {
                Timber.d("Continue from result!")
                GoogleSignIn.getSignedInAccountFromIntent(data).addOnCompleteListener(this) {
                    if (it.isSuccessful) {
                        val account = it.result
                        Timber.d("Play Games Sign in!")
                        if (account != null) {
                            onGooglePlayGamesConnected(account)
                        }
                    } else {
                        val exception = it.exception as? ApiException
                        val message = when {
                            exception == null -> R.string.invalid_google_sign_in_error
                            exception.statusCode == SIGN_IN_CANCELLED -> R.string.google_sign_in_cancelled
                            exception.statusCode == 4 -> R.string.error_connection_failed
                            exception.statusCode == 12500 -> R.string.google_sign_in_outdated
                            exception.statusCode == SIGN_IN_CURRENTLY_IN_PROGRESS -> R.string.google_sign_in_progress
                            exception.statusCode == NETWORK_ERROR -> R.string.error_connection_failed
                            exception.message.isNullOrBlank() -> R.string.invalid_google_sign_in_error
                            else -> R.string.invalid_google_sign_in_error
                        }
                        Timber.e("Exception: ${exception?.message}; ${exception?.statusCode}")
                        mGamesInstance.onDisconnected()
                        val error = getString(message)
                        showSnack(error)
                    }
                }
            }
        }
    }
}
