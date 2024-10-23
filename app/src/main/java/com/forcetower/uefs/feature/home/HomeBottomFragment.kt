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

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.BuildConfig
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeBottomFragment : UFragment() {
    @Inject lateinit var remoteConfig: FirebaseRemoteConfig

    @Inject lateinit var preferences: SharedPreferences

    private val pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
        onContentSelected(it)
    }

    private val cropImage = registerForActivityResult(CropImageContract()) {
        onCropResults(it)
    }

    private lateinit var binding: HomeBottomBinding
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return HomeBottomBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            lifecycleOwner = this@HomeBottomFragment
            viewModel = this@HomeBottomFragment.viewModel
            executePendingBindings()
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
        viewModel.databaseAccount.observe(viewLifecycleOwner) { handleAccount(it) }
    }

    private fun handleAccount(account: EdgeServiceAccount?) {
        binding.account = account
    }

    private fun featureFlags() {
        // This feature is broken, i think
        // toggleItem(R.id.demand, false)

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

    private fun onImagePicked(uri: Uri) {
        viewModel.setSelectedImage(uri)
        Glide.with(requireContext())
            .load(uri)
            .fallback(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .placeholder(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageUserPicture)

        viewModel.uploadImageToStorage()
    }

    private fun pickImage() {
        pickImageContract.launch("image/*")
    }

    private fun onContentSelected(uri: Uri?) {
        uri ?: return
        val bg = ColorUtils.modifyAlpha(ContextCompat.getColor(requireContext(), R.color.colorPrimary), 120)
        val ac = ContextCompat.getColor(requireContext(), R.color.colorAccent)

        val options = CropImageContractOptions(
            uri,
            CropImageOptions(
                fixAspectRatio = true,
                aspectRatioX = 1,
                aspectRatioY = 1,
                cropShape = CropImageView.CropShape.OVAL,
                backgroundColor = bg,
                borderLineColor = ac,
                borderCornerColor = ac,
                activityMenuIconColor = ac,
                borderLineThickness = getPixelsFromDp(requireContext(), 2),
                activityTitle = getString(R.string.cut_profile_image),
                guidelines = CropImageView.Guidelines.OFF
            )
        )

        cropImage.launch(options)
    }

    private fun onCropResults(result: CropImageView.CropResult) {
        val imageUri = result.uriContent ?: return
        onImagePicked(imageUri)
    }
}
