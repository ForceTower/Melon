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

package com.forcetower.uefs.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.databinding.ActivitySettingsBinding
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class SettingsActivity : UActivity(), HasAndroidInjector, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Any>

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
        binding.btnBack.setOnClickListener {
            supportFragmentManager.run {
                if (backStackEntryCount == 0) finish()
                else popBackStack()
            }
        }
        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                add(R.id.fragment_container, RootSettingsFragment())
            }
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference?): Boolean {
        val key = pref?.key ?: return false
        when (key) {
            "settings_synchronization" -> navigateTo(SyncSettingsFragment())
            "settings_notifications" -> {
                if (VersionUtils.isOreo()) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(intent)
                } else {
                    navigateTo(NotificationSettingsFragment())
                }
            }
            "settings_account" -> navigateTo(AccountSettingsFragment())
        }
        return true
    }

    private fun navigateTo(fragment: Fragment) {
        supportFragmentManager.inTransaction {
            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
        }
    }

    override fun androidInjector() = fragmentInjector

    companion object {
        fun startIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}