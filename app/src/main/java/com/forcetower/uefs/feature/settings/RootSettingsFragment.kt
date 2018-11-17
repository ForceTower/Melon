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

package com.forcetower.uefs.feature.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker

class RootSettingsFragment: PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener ,Injectable {
    private fun getSharedPreferences() = preferenceManager.sharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_start, rootKey)
//        addPreferencesFromResource(R.xml.settings_synchronization)
//        addPreferencesFromResource(R.xml.settings_notifications)
//        addPreferencesFromResource(R.xml.settings_account)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val intValue = newValue as? Int
        when (preference?.key) {
            "stg_sync_worker_type" -> changeWorkerType(intValue)
            "stg_sync_frequency" -> changeSyncFrequency(intValue)
            "stg_sync_auto_proxy" -> autoProxy(newValue)
            else -> Unit
        }
        return true
    }

    private fun changeWorkerType(intValue: Int?) {
        intValue?: return
        val period = getSharedPreferences().getInt("stg_sync_frequency", 60)
        when (intValue) {
            0 -> {
                SyncLinkedWorker.stopWorker()
                SyncMainWorker.createWorker(requireContext(), period)
            }
            1 -> {
                SyncMainWorker.stopWorker()
                SyncLinkedWorker.createWorker(period)
            }
        }
    }

    private fun changeSyncFrequency(period: Int?) {
        period?: return
        val worker = getSharedPreferences().getInt("stg_sync_worker_type", 0)
        if (period >= 15) {
            when (worker) {
                0 -> {
                    SyncMainWorker.stopWorker()
                    SyncMainWorker.createWorker(requireContext(), period)
                }
                1 -> SyncLinkedWorker.createWorker(period)
            }
        } else {
            SyncMainWorker.stopWorker()
            SyncLinkedWorker.createWorker(period)
            getSharedPreferences().edit().putInt("stg_sync_worker_type", 1).apply()
        }
    }

    private fun autoProxy(newValue: Any?) {
        val value = (newValue as? Boolean)?: return
        if (value) {
            
        }
    }

}