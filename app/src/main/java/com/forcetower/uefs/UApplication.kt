/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.forcetower.sagres.SagresNavigator
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.injection.AppComponent
import com.forcetower.uefs.core.injection.AppInjection
import com.forcetower.uefs.core.storage.cookies.PrefsCookiePersistor
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.forcetower.uefs.feature.themeswitcher.ThemePreferencesManager
import com.forcetower.uefs.impl.AndroidBase64Encoder
import com.forcetower.uefs.impl.CrashlyticsTree
import com.forcetower.uefs.impl.SharedPrefsCachePersistence
import com.forcetower.uefs.service.NotificationHelper
import com.google.android.play.core.missingsplits.MissingSplitsManagerFactory
import com.google.android.play.core.splitcompat.SplitCompat
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

/**
 * Representa o aplicativo por completo.
 *
 * Iniciar o aplicativo por qualquer meio irá iniciar os processos de injeção de dependençias
 */
class UApplication : Application(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject
    lateinit var preferences: SharedPreferences

    lateinit var component: AppComponent

    @Volatile
    private var injected = false

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onCreate() {
        if (MissingSplitsManagerFactory.create(this).disableAppIfMissingRequiredSplits()) {
            return
        }

        if (BuildConfig.DEBUG) {
            // Se em debug, print no logcat todas as informações
            Timber.plant(Timber.DebugTree())
        } else {
            // Em release, enviar exceptions para o crashlytics
            Timber.plant(CrashlyticsTree())
        }
        // Injeta as dependências. Este é o ponto inicial
        injectApplicationIfNecessary()
        super.onCreate()
        setupDayNightTheme(this)
        AndroidThreeTen.init(this)
        // Redefine os trabalhos de sincronização
        defineWorker()
    }

    /**
     * Este método irá observar qual o tipo de sincronização selecionado e tentará cria apenas se ele
     * tiver sido apagado
     */
    private fun defineWorker() {
        val worker = preferences.getString("stg_sync_worker_type", "0")?.toIntOrNull() ?: 0
        val period = preferences.getString("stg_sync_frequency", "60")?.toIntOrNull() ?: 60
        when (worker) {
            0 -> SyncMainWorker.createWorker(this, period)
            1 -> Unit // SyncLinkedWorker.createWorker(period, false)
        }
    }

    /**
     * Cria o componente do Dagger.
     * Este processo pode ser simplicado se a classe extendesse de DaggerApplication
     */
    private fun createApplicationInjector() = AppInjection.create(this)

    /**
     * Injetar as dependencias!
     */
    private fun injectApplicationIfNecessary() {
        if (!injected) {
            synchronized(this) {
                if (!injected) {
                    component = createApplicationInjector()
                    component.inject(this)
                    check(injected) { "Attempt to inject the app has failed" }
                }
            }
        }
    }

    /**
     * Marca aplicação como injetada
     */
    @Inject
    fun setInjected() {
        injected = true
    }

    /**
     * Inicializa o objeto de conexão com o Sagres
     */
    @Inject
    fun configureSagresNavigator() {
        val selected = preferences.getString(Constants.SELECTED_INSTITUTION_KEY, "UEFS") ?: "UEFS"
        SagresNavigator.initialize(
            PrefsCookiePersistor(this),
            selected,
            AndroidBase64Encoder(),
            SharedPrefsCachePersistence(preferences)
        )
        SagresNavigator.instance.setCookiesOnClient(".PORTALAUTH=71914E2418D3CEC72A7ED33E9055268B017E3EAB169CB4D315B8AD252D7C5BADF49E2FBF2EFE9A0E85F553DAF5CC1EB3C4C3D23A1FF7DB8C781101850A826FE14EF06FE02B5E94333BB9DB00A2EA431340AE7E7E95C2EEB96A256262C94EA542B15FDE13725D044861629DB28452D42318A4DFF6A8CEBC225886AA586D1AD8407C83CAAC0396793FB639E31DFA530992391CF3EC097B5721C2A8305D9A3385B039B08753;ASP.NET_SessionId=qaotejuupxbwhe1pjfdln3kn")
    }

    /**
     * Cria/Apaga os canais de notificação
     */
    @Inject
    fun configureNotifications() {
        NotificationHelper(this).createChannels()
    }

    override fun androidInjector() = androidInjector

    companion object {
        fun setupDayNightTheme(context: Context) {
            ThemePreferencesManager(context).run {
                applyTheme()
                retrieveOverlay()
            }
        }
    }
}