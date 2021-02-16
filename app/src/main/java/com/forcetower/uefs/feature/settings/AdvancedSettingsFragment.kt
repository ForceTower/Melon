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

package com.forcetower.uefs.feature.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.util.VersionUtils
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.work.sync.SyncMainWorker
import com.forcetower.uefs.feature.messages.MessagesDFMViewModel
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.judemanutd.autostarter.AutoStartPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by activityViewModels()
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        onPreferenceChange(shared, key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_advanced, rootKey)
        updateDozePreferences()

        if (!getSharedPreferences().isStudentFromUEFS()) {
            findPreference<SwitchPreference>("stg_advanced_aeri_tab")?.isVisible = false
            findPreference<SwitchPreference>("stg_advanced_map_tab")?.isVisible = false
        } else {
            val mapOption = remoteConfig.getBoolean("feature_flag_campus_map") || BuildConfig.VERSION_NAME.contains("-beta")
            findPreference<SwitchPreference>("stg_advanced_map_tab")?.isVisible = mapOption
        }

        findPreference<Preference>("stg_advanced_auto_start")?.let {
            it.setOnPreferenceClickListener {
                val result = AutoStartPermissionHelper.getInstance().getAutoStartPermission(requireContext())
                if (!result) {
                    Snackbar.make(requireView(), getString(R.string.settings_auto_start_manager_not_found), Snackbar.LENGTH_SHORT).show()
                }
                true
            }
        }

        findPreference<Preference>("stg_advanced_battery_optimization")?.let {
            it.setOnPreferenceClickListener {
                CustomTabActivityHelper.openCustomTab(
                    requireActivity(),
                    CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(
                            CustomTabColorSchemeParams
                                .Builder()
                                .setToolbarColor(ViewUtils.attributeColorUtils(requireContext(), R.attr.colorPrimary))
                                .build()
                        )
                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                        .build(),
                    Uri.parse("https://dontkillmyapp.com/${Build.BRAND.toLowerCase(Locale.getDefault())}")
                )
                true
            }
        }

        findPreference<PreferenceCategory>("stg_cat_dev_tools")?.let {
            val app = (requireContext().applicationContext as UApplication)
            it.isVisible = app.disciplineToolbarDevClickCount == 15 && app.messageToolbarDevClickCount == 15 || BuildConfig.DEBUG
        }

        findPreference<Preference>("stg_crash_app")?.let {
            it.setOnPreferenceClickListener {
                throw IllegalStateException("Crash request delivered :D")
            }
        }

        findPreference<Preference>("stg_share_main_database")?.let {
            it.setOnPreferenceClickListener {
                obtainMainDatabase()
                true
            }
        }
    }

    private fun obtainMainDatabase() {
        val file = requireContext().getDatabasePath("unespiercer.db")
        val targetFolder = File(requireContext().getExternalFilesDir(null), "databases")
        if (!targetFolder.exists()) targetFolder.mkdirs()

        val target = File(targetFolder, "${System.currentTimeMillis()}.db")
        target.createNewFile()

        // this block will freeze the app
        file.copyTo(target, overwrite = true)

        val uri = FileProvider.getUriForFile(requireContext().applicationContext, BuildConfig.APPLICATION_ID + ".fileprovider", target)
        val intent = ShareCompat.IntentBuilder(requireContext())
            .setType("application/vnd.sqlite3")
            .setStream(uri)
            .setChooserTitle(getString(R.string.share_file))
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Main Database share error")
            Toast.makeText(requireContext(), "Erro ao enviar arquivo, mas ele está na pasta", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDozePreferences() {
        when (Build.BRAND.toLowerCase(Locale.getDefault())) {
            "xiaomi", "redmi" -> updateXiaomiBattery()
        }
        updateDefaultBattery()
    }

    private fun updateXiaomiBattery() {
        val preference = findPreference<Preference>("stg_advanced_ignore_battery_xiaomi") ?: return
        preference.isVisible = true
        preference.setOnPreferenceClickListener {
            powerXiaomi()
            true
        }
    }

    private fun updateDefaultBattery() {
        val preference = findPreference<SwitchPreference>("stg_advanced_ignore_doze") ?: return
        preference.isVisible = true
        if (VersionUtils.isMarshmallow()) {
            preference.let {
                val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager?
                val ignoring = pm?.isIgnoringBatteryOptimizations(requireContext().packageName)
                    ?: false
                it.isChecked = ignoring
            }
        } else {
            preference.let {
                it.isEnabled = false
                it.setSummary(R.string.settings_adv_doze_mode_info_disabled)
            }
        }
    }

    private fun onPreferenceChange(preference: SharedPreferences, key: String) {
        when (key) {
            "stg_advanced_aeri_tab" -> aeriTabs(preference.getBoolean(key, true))
            "stg_advanced_maps_install" -> mapsInstall(preference.getBoolean(key, true))
            "stg_advanced_ignore_doze" -> dozeDefault(preference.getBoolean(key, false))
        }
    }

    private fun aeriTabs(enabled: Boolean) {
        if (!enabled) {
            viewModel.uninstallModuleIfExists(MessagesDFMViewModel.AERI_MODULE)
        }
    }

    private fun mapsInstall(enabled: Boolean) {
        if (!enabled) {
            viewModel.uninstallModuleIfExists(Constants.DynamicFeatures.MAPS)
        }
    }

    private fun powerXiaomi() {
        try {
            val intent = Intent()
            intent.component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
            intent.putExtra("package_name", requireContext().packageName)
            intent.putExtra("package_label", getText(R.string.app_name))
            // Uses the same code because frequency will be reset on completion
            startActivityForResult(intent, REQUEST_IGNORE_DOZE)
        } catch (throwable: ActivityNotFoundException) {
            Timber.e(throwable, "Xiaomi without a power keeper")
            dozeDefault(true)
        }
    }

    @SuppressLint("BatteryLife")
    private fun dozeDefault(ignore: Boolean) {
        if (!VersionUtils.isMarshmallow()) return
        if (!ignore) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } else {
            val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager?
            val ignoring = pm?.isIgnoringBatteryOptimizations(requireContext().packageName) ?: false
            Timber.d("Ignoring? $ignoring")
            if (!ignoring) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context?.packageName}")
                }
                startActivityForResult(intent, REQUEST_IGNORE_DOZE)
            }
        }
    }

    private fun setDozePreference(enabled: Boolean) {
        findPreference<SwitchPreference>("stg_advanced_ignore_doze")?.let {
            it.isChecked = enabled
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(listener)
        updateDozePreferences()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getSharedPreferences() = preferenceManager.sharedPreferences

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_IGNORE_DOZE -> {
                if (resultCode == Activity.RESULT_OK) {
                    SyncMainWorker.stopWorker(requireContext())
                    SyncMainWorker.createWorker(requireContext(), 60)
                    Snackbar.make(requireView(), getString(R.string.settings_accept_ignore_doze), Snackbar.LENGTH_LONG).show()
                    getSharedPreferences().edit().putString("stg_sync_frequency", "60").apply()
                    setDozePreference(true)
                } else {
                    Snackbar.make(requireView(), getString(R.string.settings_refuse_ignore_doze), Snackbar.LENGTH_LONG).show()
                    getSharedPreferences().edit().putBoolean("stg_advanced_ignore_doze", false).apply()
                    setDozePreference(false)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_IGNORE_DOZE = 40000
    }
}
