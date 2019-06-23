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

import android.app.Activity
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.util.ColorUtils
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.HomeBottomBinding
import com.forcetower.uefs.feature.about.AboutActivity
import com.forcetower.uefs.feature.feedback.SendFeedbackFragment
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.forcetower.uefs.feature.setup.CourseSelectionCallback
import com.forcetower.uefs.feature.setup.SelectCourseDialog
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import javax.inject.Inject

class HomeBottomFragment : UFragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: UViewModelFactory
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
            lifecycleOwner = this@HomeBottomFragment
            viewModel = this@HomeBottomFragment.viewModel
            executePendingBindings()
            imageUserPicture.setOnClickListener { pickImage() }
            textUserName.setOnClickListener { editCourse() }
            textScore.setOnClickListener { editCourse() }
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

    private fun editCourse() {
        if (!preferences.isStudentFromUEFS()) return
        val dialog = SelectCourseDialog()
        dialog.setCallback(object : CourseSelectionCallback {
            override fun onSelected(course: Course) {
                viewModel.setSelectedCourse(course)
            }
        })
        dialog.show(childFragmentManager, "dialog_course")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNavigation()
        featureFlags()
        viewModel.account.observe(this, Observer { handleAccount(it) })
    }

    private fun handleAccount(resource: Resource<Account>) {
        val data = resource.data ?: return
        toggleNightModeSwitcher(data.darkThemeEnabled)
        binding.account = data
    }

    private fun featureFlags() {
        val demandFlag = remoteConfig.getBoolean("feature_flag_demand")
        viewModel.flags.observe(this, Observer {
            if (it?.demandOpen == true || demandFlag) {
                toggleItem(R.id.demand, true)
            }
        })

        val uefsStudent = preferences.isStudentFromUEFS()

        val storeFlag = remoteConfig.getBoolean("feature_flag_store")
        toggleItem(R.id.purchases, storeFlag)

        val dark = preferences.getBoolean("stg_night_mode_menu", true)
        toggleItem(R.id.dark_theme_event, uefsStudent && dark)

        val hourglass = remoteConfig.getBoolean("feature_flag_evaluation") && uefsStudent
        toggleItem(R.id.evaluation, hourglass)

        toggleItem(R.id.adventure, uefsStudent)
        toggleItem(R.id.big_tray, uefsStudent)
        toggleItem(R.id.events, uefsStudent)
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
                    val fragment = LogoutConfirmationFragment()
                    fragment.show(childFragmentManager, "logout_modal")
                    true
                }
                R.id.open_source -> {
                    LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.DARK)
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
                    val fragment = SendFeedbackFragment()
                    fragment.show(childFragmentManager, "feedback_modal")
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(item, findNavController())
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_SELECT_PICTURE)
    }

    private fun onImagePicked(uri: Uri) {
        viewModel.setSelectedImage(uri)
        GlideApp.with(requireContext())
            .load(uri)
            .fallback(R.mipmap.ic_unes_large_image_512)
            .placeholder(R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageUserPicture)

        viewModel.uploadImageToStorage()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECT_PICTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                    val uri = data.data!!

                    val bg = ColorUtils.modifyAlpha(ContextCompat.getColor(requireContext(), R.color.colorPrimary), 120)
                    val ac = ContextCompat.getColor(requireContext(), R.color.colorAccent)
                    CropImage.activity(uri)
                            .setFixAspectRatio(true)
                            .setAspectRatio(1, 1)
                            .setCropShape(CropImageView.CropShape.OVAL)
                            .setBackgroundColor(bg)
                            .setBorderLineColor(ac)
                            .setBorderCornerColor(ac)
                            .setActivityMenuIconColor(ac)
                            .setBorderLineThickness(getPixelsFromDp(requireContext(), 2))
                            .setActivityTitle(getString(R.string.cut_profile_image))
                            .setGuidelines(CropImageView.Guidelines.OFF)
                            .start(requireContext(), this)
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == Activity.RESULT_OK) {
                    val imageUri = result.uri
                    onImagePicked(imageUri)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_SELECT_PICTURE = 8000
    }
}