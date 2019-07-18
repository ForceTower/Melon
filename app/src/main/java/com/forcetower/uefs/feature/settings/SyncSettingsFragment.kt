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

package com.forcetower.uefs.feature.settings

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.forcetower.uefs.R
import com.forcetower.uefs.RC_LOCATION_PERMISSION
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.storage.repository.FirebaseAuthRepository
import com.forcetower.uefs.core.storage.repository.SyncFrequencyRepository
import com.forcetower.uefs.core.work.sync.SyncLinkedWorker
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import javax.inject.Inject

class SyncSettingsFragment : PreferenceFragmentCompat(), Injectable {
    @Inject
    lateinit var firebaseRepository: FirebaseAuthRepository
    @Inject
    lateinit var repository: SyncFrequencyRepository

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_synchronization, rootKey)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        repository.getFrequencies().observe(this, Observer { frequencies ->
            val entries = frequencies.map { it.name }
            val values = frequencies.map { it.value.toString() }
            configureFrequencies(entries, values)
        })
    }

    private fun configureFrequencies(entries: List<String>, values: List<String>) {
        val preference = findPreference("stg_sync_frequency") as? ListPreference
        preference ?: return
        preference.entries = entries.toTypedArray()
        preference.entryValues = values.toTypedArray()
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String) {
        when (key) {
            "stg_sync_worker_type" -> changeWorkerType(preference.getString(key, "0")?.toIntOrNull())
            "stg_sync_frequency" -> changeSyncFrequency(preference.getString(key, "60")?.toIntOrNull())
            "stg_sync_auto_proxy" -> autoProxy(preference.getBoolean(key, false))
            "stg_sync_proxy" -> proxySettings(preference.getString(key, "10.65.16.2:3128")!!)
            else -> Timber.d("Else... $key")
        }
    }

    private fun proxySettings(proxy: String) {
        val splits = proxy.split(":")
        var valid = false
        if (splits.size == 2) {
            val port = splits[1].toIntOrNull()
            if (port != null) {
                valid = true
            }
        }

        if (!valid) {
            Toast.makeText(requireContext(), R.string.settings_proxy_invalid, Toast.LENGTH_SHORT).show()
            getSharedPreferences().edit().putString("stg_sync_proxy", "10.65.16.2:3128").apply()
        } else {
            Toast.makeText(requireContext(), R.string.settings_proxy_changed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeWorkerType(intValue: Int?): Boolean {
        intValue ?: return false
        Timber.d("Changing worker type to $intValue")
        var period = getSharedPreferences().getString("stg_sync_frequency", "60")?.toIntOrNull() ?: 60
        when (intValue) {
            0 -> {
                SyncLinkedWorker.stopWorker(requireContext())
                if (period < 15) {
                    period = 15
                    getSharedPreferences().edit().putString("stg_sync_frequency", "15").apply()
                }
                SyncMainWorker.createWorker(requireContext(), period)
            }
            1 -> {
                SyncMainWorker.stopWorker(requireContext())
                SyncLinkedWorker.createWorker(requireContext(), period)
            }
        }
        firebaseRepository.updateFrequency(period)
        return true
    }

    private fun changeSyncFrequency(period: Int?): Boolean {
        period ?: return false
        val worker = getSharedPreferences().getString("stg_sync_worker_type", "0")?.toIntOrNull() ?: 0
        if (period >= 15) {
            when (worker) {
                0 -> {
                    SyncMainWorker.stopWorker(requireContext())
                    SyncMainWorker.createWorker(requireContext(), period)
                }
                1 -> SyncLinkedWorker.createWorker(requireContext(), period)
            }
        } else {
            SyncMainWorker.stopWorker(requireContext())
            SyncLinkedWorker.stopWorker(requireContext())
            SyncLinkedWorker.createWorker(requireContext(), period)
            getSharedPreferences().edit().putString("stg_sync_worker_type", "1").apply()
        }

        firebaseRepository.updateFrequency(period)
        return true
    }

    private fun autoProxy(value: Boolean): Boolean {
        return if (value) {
            checkAndEnableAutoProxy()
            false
        } else {
            true
        }
    }

    private fun enableAutoProxy() {
        getSharedPreferences().edit().putBoolean("stg_sync_auto_proxy", true).apply()
        (findPreference("stg_sync_auto_proxy") as? TwoStatePreference).also {
            Timber.d("was it converted? ${it == null}")
        }?.isChecked = true
    }

    @AfterPermissionGranted(RC_LOCATION_PERMISSION)
    private fun checkAndEnableAutoProxy() {
        val perms = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        if (EasyPermissions.hasPermissions(requireContext(), *perms)) {
            enableAutoProxy()
        } else {
            getSharedPreferences().edit().putBoolean("stg_sync_auto_proxy", false).apply()
            (findPreference("stg_sync_auto_proxy") as? TwoStatePreference)?.isChecked = false
            EasyPermissions.requestPermissions(this, getString(R.string.permission_location_sync_text), RC_LOCATION_PERMISSION, *perms)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences
}