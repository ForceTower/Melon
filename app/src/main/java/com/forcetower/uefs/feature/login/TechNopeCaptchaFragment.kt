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

package com.forcetower.uefs.feature.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.feature.shared.UFragment
import javax.inject.Inject

class TechNopeCaptchaFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // create a layout that is basically a webview, monitor page progress and redirect.
        // catch the cookies and set to the navigator
        // import okhttp3.Cookie;
        // SagresNavigator.instance.setCookies(List<Cookie>) ...... this method will be available on next version of navigator
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}