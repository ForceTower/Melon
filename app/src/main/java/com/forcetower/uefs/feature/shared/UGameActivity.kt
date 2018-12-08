package com.forcetower.uefs.feature.shared

import android.content.Intent
import android.os.Bundle
import com.forcetower.uefs.GooglePlayGamesInstance
import com.forcetower.uefs.PLAY_GAMES_ACHIEVEMENTS
import com.forcetower.uefs.PLAY_GAMES_SIGN_IN
import com.forcetower.uefs.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.*
import com.google.android.gms.common.api.ApiException
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
        startActivityForResult(client.signInIntent, PLAY_GAMES_SIGN_IN)
    }

    private fun onGooglePlayGamesConnected(account: GoogleSignInAccount) {
        mGamesInstance.onConnected(account)
        mGamesInstance.gamesClient?.setViewForPopups(window.decorView.findViewById(android.R.id.content))
        checkAchievements()
    }

    open fun checkAchievements() = Unit

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
            }
            showSnack(getString(R.string.unable_to_open_achievements))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PLAY_GAMES_SIGN_IN -> {
                GoogleSignIn.getSignedInAccountFromIntent(data).addOnCompleteListener(this) {
                    if (it.isSuccessful) {
                        val account = it.result
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
                        mGamesInstance.onDisconnected()
                        val error = getString(message)
                        showSnack(error)
                    }
                }
            }
        }
    }

}