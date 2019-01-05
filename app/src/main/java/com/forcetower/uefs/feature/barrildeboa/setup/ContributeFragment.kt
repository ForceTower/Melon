/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.feature.barrildeboa.setup

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.databinding.FragmentHourglassContributeBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class ContributeFragment : UFragment(), Injectable {
    @Inject
    lateinit var analytics: FirebaseAnalytics
    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentHourglassContributeBinding.inflate(inflater, container, false).apply {
            doNotContributeButton.setOnClickListener {
                val dialog = HourglassNotContributeDialog()
                dialog.show(childFragmentManager, "hourglass_not_contribute_dialog")
                analytics.logEvent("hourglass_not_contribute_wannabe", null)
            }
            confirmButton.setOnClickListener {
                preferences.edit().putInt("hourglass_state", 2).apply()
                analytics.logEvent("hourglass_contribute", null)
                // findNavController().navigate(R.id.initial_screen)
            }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        analytics.logEvent("hourglass_contribute_screen", null)
    }
}