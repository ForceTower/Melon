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

package com.forcetower.uefs.feature.home

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.constants.Constants
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.HomeBottomBinding
import com.forcetower.uefs.feature.about.AboutActivity
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import javax.inject.Inject

class HomeBottomFragment : UFragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage
    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var binding: HomeBottomBinding
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(viewModelFactory)

        return HomeBottomBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            setLifecycleOwner(this@HomeBottomFragment)
            viewModel = this@HomeBottomFragment.viewModel
            firebaseStorage = this@HomeBottomFragment.firebaseStorage
            firebaseUser = this@HomeBottomFragment.firebaseAuth.currentUser
            executePendingBindings()
            imageUserPicture.setOnClickListener { this@HomeBottomFragment.viewModel.onMeProfileClicked() }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        nightSwitcherListener()
    }

    private fun nightSwitcherListener() {
        val config = preferences.getString("stg_night_mode", "0")?.toIntOrNull() ?: 0
        val active = config == 2
        binding.switchNight.isChecked = active
        binding.switchNight.setOnCheckedChangeListener { _, isChecked ->
            val flag = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            val state = if (isChecked) 2 else 1
            preferences.edit().putString("stg_night_mode", state.toString()).apply()
            AppCompatDelegate.setDefaultNightMode(flag)
            activity?.recreate()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNavigation()
        featureFlags()
        viewModel.isDarkModeEnabled.observe(this, Observer { toggleNightModeSwitcher(it) })
    }

    private fun featureFlags() {
        val demandFlag = remoteConfig.getBoolean("feature_flag_demand")
        viewModel.flags.observe(this, Observer {
            if (it?.demandOpen == true || demandFlag) {
                toggleItem(R.id.demand, true)
            }
        })

        val storeFlag = remoteConfig.getBoolean("feature_flag_store")
        toggleItem(R.id.purchases, storeFlag)

        val hourglass = remoteConfig.getBoolean("feature_flag_hourglass")
        toggleItem(R.id.hourglass, hourglass)
    }

    private fun toggleNightModeSwitcher(enabled: Boolean?) {
        binding.switchNight.run {
            visibility = if (enabled == true) VISIBLE
            else GONE
        }
    }

    private fun toggleItem(@IdRes id: Int, visible: Boolean) {
        val item = binding.navigationView.menu.findItem(id)
        item?.isVisible = visible
    }

    private fun setupNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.about -> {
                    AboutActivity.startActivity(requireActivity())
                    true
                }
                R.id.logout -> {
                    viewModel.logout()
                    true
                }
                R.id.open_source -> {
                    LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT)
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .withAboutDescription(getString(R.string.about_description))
                        .start(requireContext())
                    true
                }
                R.id.profile -> {
                    viewModel.onMeProfileClicked()
                    true
                }
                R.id.settings -> {
                    startActivity(SettingsActivity.startIntent(requireContext()))
                    true
                }
                R.id.bug_report -> {
                    val text = "\n\nVersion: ${BuildConfig.VERSION_NAME}\nCode: ${BuildConfig.VERSION_CODE}"
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", Constants.DEVELOPER_EMAIL, null)).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "[UNES]App_Feedback")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(item, findNavController())
            }
        }
        // NavigationUI.setupWithNavController(binding.navigationView, findNavController())
    }
}