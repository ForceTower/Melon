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

package com.forcetower.uefs.core.injection

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.UApplication
import dagger.android.AndroidInjection
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection

object AppInjection {
    /**
     * Registra um evento para descobrir quando uma atividade foi criada para que possamos injetar
     * as dependencias nela
     */
    fun create(application: UApplication): AppComponent {
        application.registerActivityLifecycleCallbacks(object : ActLifecycleCbAdapter() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                handle(activity)
            }
        })
        // Retorna o componente do Dagger [Observe que existe uma classe chamada AppComponent no
        // modulo core.injection
        return DaggerAppComponent.builder().application(application).build()
    }

    /**
     * Para cada fragmento gerado dentro das atividades, escute o evento de ligação e injeta
     * as dependencias nos fragmentos tambem
     */
    private fun handle(activity: Activity?) {
        if (activity is HasAndroidInjector) {
            AndroidInjection.inject(activity)
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
                        if (f is Injectable) AndroidSupportInjection.inject(f)
                    }
                }, true)
            }
        }
    }
}