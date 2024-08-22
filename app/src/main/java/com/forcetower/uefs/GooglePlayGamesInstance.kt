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

import android.app.Activity
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.forcetower.core.lifecycle.Event
import com.google.android.gms.games.PlayGames
import kotlinx.coroutines.tasks.await

class GooglePlayGamesInstance(
    private val activity: Activity
) : ContextWrapper(activity) {
    private var playerName: String? = null
    val signInClient = PlayGames.getGamesSignInClient(activity)
    val achievementsClient = PlayGames.getAchievementsClient(activity)

    private val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    private var playerUnlockedSwitch: Boolean = false

    private val _status = MutableLiveData<Event<GameConnectionStatus>>()
    val connectionStatus: LiveData<Event<GameConnectionStatus>>
        get() = _status

    /**
     * Deve ser chamado quando o usuário de conectar com a conta do google para que o objeto possa
     * criar todas as dependencias
     */
    fun onConnected() {
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
        _status.postValue(Event(GameConnectionStatus.DISCONNECTED))
        preferences.edit().putBoolean("google_play_games_enabled_v2", false).apply()
    }

    /**
     * Chame este método para desconectar o usuário do google play games
     */
    fun disconnect() {
        // removed. i guess
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
    suspend fun isConnected(): Boolean {
        return PlayGames.getGamesSignInClient(activity).isAuthenticated.await().isAuthenticated
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

    fun unlockAchievement(achievement: String) {
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

    fun updateProgress(id: String, value: Int) {
        achievementsClient?.setSteps(id, value)
    }
}

enum class GameConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    LOADING
}
