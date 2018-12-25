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

package com.forcetower.uefs.feature.setup

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.util.ColorUtils
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentSetupIntroductionBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import timber.log.Timber
import javax.inject.Inject

class IntroductionFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    private lateinit var binding: FragmentSetupIntroductionBinding
    private lateinit var viewModel: SetupViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentSetupIntroductionBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textSelectCourseInternal.setOnClickListener {
            val dialog = SelectCourseDialog()
            dialog.setCallback(object : CourseSelectionCallback {
                override fun onSelected(course: Course) {
                    viewModel.setSelectedCourse(course)
                    binding.textSelectCourseInternal.setText(course.name)
                }
            })
            dialog.show(childFragmentManager, "dialog_course")
        }

        binding.imageUserImage.setOnClickListener {
            pickImage()
        }

        binding.btnNext.setOnClickListener {
            val course = viewModel.getSelectedCourse()
            if (course == null) {
                binding.textSelectCourseInternal.error = getString(R.string.error_select_a_course)
            } else {
                binding.textSelectCourseInternal.error = null
                val user = firebaseAuth.currentUser
                if (user != null) {
                    viewModel.uploadImageToStorage("users/${user.uid}/avatar.jpg")
                    viewModel.updateCourse(course, user)
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
                .into(binding.imageUserImage)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECT_PICTURE -> {
                if (resultCode == RESULT_OK && data != null && data.data != null) {
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
                if (resultCode == RESULT_OK) {
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