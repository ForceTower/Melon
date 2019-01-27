/*
 * Copyright (c) 2019.
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
    fun revealAchievement(@StringRes id: Int) = mGamesInstance.revealAchievement(id)
    fun incrementAchievementProgress(@StringRes id: Int, step: Int) = mGamesInstance.incrementAchievement(id, step)
    fun updateAchievementProgress(@StringRes id: Int, value: Int) = mGamesInstance.updateProgress(id, value)
    fun signOut() = mGamesInstance.disconnect()

    fun openAchievements() {
        if (!mGamesInstance.isConnected()) return
        val client = mGamesInstance.achievementsClient
        client ?: return

        client.achievementsIntent.addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = it.result
                if (intent != null) {
                    startActivityForResult(intent, PLAY_GAMES_ACHIEVEMENTS)
                    return@addOnCompleteListener
                }
            } else {
                showSnack("${getString(R.string.unable_to_open_achievements)} ${it.exception?.message}")
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
                        Timber.e("Exception: ${exception?.message}")
                        mGamesInstance.onDisconnected()
                        val error = getString(message)
                        showSnack(error)
                    }
                }
            }
        }
    }
}