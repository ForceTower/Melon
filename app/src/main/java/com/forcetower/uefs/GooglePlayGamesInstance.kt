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

package com.forcetower.uefs

import android.content.Context
import android.content.ContextWrapper
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.forcetower.uefs.core.vm.Event
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.AchievementsClient
import com.google.android.gms.games.Games
import com.google.android.gms.games.GamesClient
import com.google.android.gms.games.LeaderboardsClient

class GooglePlayGamesInstance(base: Context) : ContextWrapper(base) {
    var playerName: String? = null
    var signInClient: GoogleSignInClient? = null
    var achievementsClient: AchievementsClient? = null
    var leaderboardClient: LeaderboardsClient? = null
    var gamesClient: GamesClient? = null

    private val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    private var playerUnlockedSwitch: Boolean = false

    private val _status = MutableLiveData<Event<GameConnectionStatus>>()
    val connectionStatus: LiveData<Event<GameConnectionStatus>>
        get() = _status

    /**
     * Cria o cliente de login com as ferramentas do Google para esta sessão de jogos!
     */
    fun createGoogleClient() {
        if (signInClient == null) {
            signInClient = GoogleSignIn.getClient(
                this,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build()
            )
        }
    }

    /**
     * Deve ser chamado quando o usuário de conectar com a conta do google para que o objeto possa
     * criar todas as dependencias
     */
    fun onConnected(account: GoogleSignInAccount) {
        achievementsClient = Games.getAchievementsClient(this, account)
        leaderboardClient = Games.getLeaderboardsClient(this, account)
        gamesClient = Games.getGamesClient(this, account)
        _status.postValue(Event(GameConnectionStatus.CONNECTED))
        preferences.edit().putBoolean("google_play_games_enabled_v2", true).apply()
        unlockAchievement(R.string.achievement_comeou_o_jogo)
        if (playerUnlockedSwitch) {
            unlockAchievement(R.string.achievement_agora_eu_entendi_agora_eu_saquei)
        }
    }

    /**
     * Deve ser chamado quando o usuário optar por sair do jogo
     */
    fun onDisconnected() {
        achievementsClient = null
        leaderboardClient = null
        gamesClient = null
        _status.postValue(Event(GameConnectionStatus.DISCONNECTED))
        preferences.edit().putBoolean("google_play_games_enabled_v2", false).apply()
    }

    /**
     * Chame este método para desconectar o usuário do google play games
     */
    fun disconnect() {
        signInClient?.signOut()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onDisconnected()
            }
        }
    }

    /**
     * Chame este método quando ocorrer uma troca de conta... Por causa de motivos...
     */
    fun changePlayerName(other: String) {
        playerUnlockedSwitch = playerName != null && other != playerName
        playerName = other
        if (playerUnlockedSwitch) {
            unlockAchievement(R.string.achievement_agora_eu_entendi_agora_eu_saquei)
        }
    }

    /**
     * Retorna se existe alguem conectado às contas do google ou não
     */
    fun isConnected(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(this) != null
    }

    /**
     * Descobre se o Google Play Games está ativado ou desativado
     */
    fun isPlayGamesEnabled(): Boolean {
        return preferences.getBoolean("google_play_games_enabled_v2", false)
    }

    /**
     * Desbloqueia uma conquista
     */
    fun unlockAchievement(@StringRes resource: Int) {
        unlockAchievement(getString(resource))
    }

    private fun unlockAchievement(achievement: String) {
        achievementsClient?.unlock(achievement)
    }

    fun revealAchievement(@StringRes resource: Int) {
        val id = getString(resource)
        achievementsClient?.reveal(id)
    }

    fun incrementAchievement(@StringRes resource: Int, step: Int) {
        val id = getString(resource)
        achievementsClient?.increment(id, step)
    }

    fun updateProgress(@StringRes resource: Int, value: Int) {
        val id = getString(resource)
        achievementsClient?.setSteps(id, value)
    }
}

enum class GameConnectionStatus {
    CONNECTED, DISCONNECTED, LOADING
}