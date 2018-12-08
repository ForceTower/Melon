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
    fun checkAchievements()


    /**
     * Verifica se o usuário está conectado ao Google Play Games
     */
    fun isConnected() : Boolean
}