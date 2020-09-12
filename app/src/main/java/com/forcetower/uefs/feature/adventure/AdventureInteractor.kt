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

package com.forcetower.uefs.feature.adventure

/**
 * Esta classe tem o propósito de intermediar a Interface Gráfica da camada de acesso aos dados,
 * neste caso, o ViewModel.
 */
interface AdventureInteractor {
    /**
     * Chamado quando o usuário diz que quer começar a aventura!
     */
    fun beginAdventure()

    /**
     * Desconecta o usuário do Google Play Games
     */
    fun leave()

    /**
     * Começa ou finaliza a requisição por localizações e verifica se a localização é de uma das
     * conquistas
     */
    fun turnOnLocations()

    /**
     * Abre as conquistas do UNES
     */
    fun openAchievements()

    /**
     * Verifica se o usuário está conectado ao Google Play Games
     */
    fun isConnected(): Boolean
}
