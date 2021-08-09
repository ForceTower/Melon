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

package com.forcetower.uefs.feature.home

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.HomeBottomBinding
import com.forcetower.uefs.feature.about.AboutActivity
import com.forcetower.uefs.feature.feedback.SendFeedbackFragment
import com.forcetower.uefs.feature.settings.SettingsActivity
import com.forcetower.uefs.feature.setup.CourseSelectionCallback
import com.forcetower.uefs.feature.setup.SelectCourseDialog
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.mikepenz.aboutlibraries.LibsBuilder
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeBottomFragment : UFragment() {
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig
    @Inject lateinit var preferences: SharedPreferences

    private lateinit var binding: HomeBottomBinding
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

    private fun editCourse() {
        if (!preferences.isStudentFromUEFS()) return
        val dialog = SelectCourseDialog()
        dialog.setCallback(
            object : CourseSelectionCallback {
                override fun onSelected(course: Course) {
                    viewModel.setSelectedCourse(course)
                }
            }
        )
        dialog.show(childFragmentManager, "dialog_course")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
        featureFlags()
        viewModel.databaseAccount.observe(viewLifecycleOwner, { handleAccount(it) })
    }

    private fun handleAccount(account: Account?) {
        binding.account = account
    }

    private fun featureFlags() {
        val demandFlag = remoteConfig.getBoolean("feature_flag_demand")
        val demandCommandFlag = remoteConfig.getBoolean("feature_flag_demand_commander")
        viewModel.flags.observe(viewLifecycleOwner) {
            if (demandCommandFlag && (it?.demandOpen == true || demandFlag)) {
                toggleItem(R.id.demand, true)
            }
        }

        val uefsStudent = preferences.isStudentFromUEFS()

        val documentsFlag = remoteConfig.getBoolean("feature_flag_documents") || BuildConfig.DEBUG
        toggleItem(R.id.documents, documentsFlag)

        val storeFlag = remoteConfig.getBoolean("feature_flag_store")
        toggleItem(R.id.purchases, storeFlag)

        val hourglass = remoteConfig.getBoolean("feature_flag_evaluation") && uefsStudent
        toggleItem(R.id.evaluation, hourglass)

        val bigTray = remoteConfig.getBoolean("feature_flag_big_tray") && uefsStudent
        toggleItem(R.id.big_tray, bigTray)

        val themeSwitcher = remoteConfig.getBoolean("feature_flag_theme_switcher")
        toggleItem(R.id.theme_switcher, themeSwitcher)

        val campusMap = (remoteConfig.getBoolean("feature_flag_campus_map") || BuildConfig.VERSION_NAME.contains("-beta")) && uefsStudent
        val campusPreference = preferences.getBoolean("stg_advanced_maps_install", true)
        toggleItem(R.id.campus_map, campusMap && campusPreference)

        toggleItem(R.id.adventure, uefsStudent)
        toggleItem(R.id.events, uefsStudent)
        toggleItem(R.id.flowchart, uefsStudent)
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
                        .withEdgeToEdge(true)
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .withAboutDescription(getString(R.string.about_description))
                        .start(requireContext())
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
                R.id.campus_map -> {
                    findNavController().navigate(R.id.campus_map)
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, findNavController())
                }
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
