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

package com.forcetower.uefs.feature.setup

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.repository.SyncFrequencyRepository
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.FragmentSetupIntroductionBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class IntroductionFragment : UFragment() {
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseStorage: FirebaseStorage
    @Inject lateinit var repository: SyncFrequencyRepository
    @Inject lateinit var preferences: SharedPreferences

    private val pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
        onContentSelected(it)
    }

    private val cropImage = registerForActivityResult(CropImageContract()) {
        onCropResults(it)
    }

    private val viewModel: SetupViewModel by activityViewModels()
    private lateinit var binding: FragmentSetupIntroductionBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentSetupIntroductionBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uefsStudent = preferences.isStudentFromUEFS()
        if (!uefsStudent) binding.textSelectCourse.visibility = View.INVISIBLE
        binding.textSelectCourseInternal.setOnClickListener {
            val dialog = SelectCourseDialog()
            dialog.setCallback(
                object : CourseSelectionCallback {
                    override fun onSelected(course: Course) {
                        viewModel.setSelectedCourse(course)
                        binding.textSelectCourseInternal.setText(course.name)
                    }
                }
            )
            dialog.show(childFragmentManager, "dialog_course")
        }

        binding.imageUserImage.setOnClickListener {
            pickImage()
        }

        binding.btnNext.setOnClickListener {
            val course = viewModel.getSelectedCourse()
            if (course == null && uefsStudent) {
                binding.textSelectCourseInternal.error = getString(R.string.error_select_a_course)
            } else {
                binding.textSelectCourseInternal.error = null
                val user = firebaseAuth.currentUser
                if (user != null) {
                    viewModel.uploadImageToStorage()
                    if (uefsStudent) {
                        viewModel.updateCourse(course, user)
                    }
                } else {
                    Timber.d("Not connected to firebase. Write would denied")
                }
                findNavController().navigate(R.id.action_introduction_to_configuration)
            }
        }

        val current = firebaseAuth.currentUser
        if (current != null) {
            val reference = firebaseStorage.getReference("users/${current.uid}/avatar.jpg")
            GlideApp.with(requireContext())
                .load(reference)
                .fallback(R.drawable.ic_account_black_24dp)
                .placeholder(R.drawable.ic_account_black_24dp)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .signature(ObjectKey(System.currentTimeMillis() ushr 21))
                .into(binding.imageUserImage)
        }

        repository.getFrequencies().observe(
            viewLifecycleOwner
        ) {
            viewModel.syncFrequencies = it
        }
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

    private fun onImagePicked(uri: Uri) {
        viewModel.setSelectedImage(uri)
        GlideApp.with(requireContext())
            .load(uri)
            .fallback(R.mipmap.ic_unes_large_image_512)
            .placeholder(R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageUserImage)
    }
}
