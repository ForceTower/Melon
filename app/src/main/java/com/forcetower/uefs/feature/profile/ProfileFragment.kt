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

package com.forcetower.uefs.feature.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.util.ColorUtils
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentProfileBinding
import com.forcetower.uefs.feature.profile.ProfileActivity.Companion.EXTRA_PROFILE_ID
import com.forcetower.uefs.feature.setup.SetupViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.postponeEnterTransition
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.siecomp.session.PushUpScrollListener
import com.forcetower.uefs.feature.siecomp.speaker.ImageLoadListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import javax.inject.Inject

class ProfileFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    @Inject
    lateinit var firebaseStorage: FirebaseStorage

    private lateinit var viewModel: ProfileViewModel
    private lateinit var binding: FragmentProfileBinding
    private lateinit var setupViewModel: SetupViewModel
    private lateinit var adapter: ProfileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        setupViewModel = provideViewModel(factory)
        activity?.postponeEnterTransition(500L)
        viewModel.setProfileId(requireNotNull(arguments).getLong(EXTRA_PROFILE_ID, 0))
        viewModel.profile.observe(this, Observer {
            if (it?.imageUrl == null) {
                activity?.startPostponedEnterTransition()
            }
        })

        val headLoadListener = object : ImageLoadListener {
            override fun onImageLoaded() { activity?.startPostponedEnterTransition() }
            override fun onImageLoadFailed() { activity?.startPostponedEnterTransition() }
        }

        adapter = ProfileAdapter(viewModel, this, headLoadListener)
        return FragmentProfileBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            lifecycleOwner = this@ProfileFragment
            viewModel = this@ProfileFragment.viewModel
            executePendingBindings()
            profileRecycler.apply {
                adapter = this@ProfileFragment.adapter
                itemAnimator?.run {
                    addDuration = 120L
                    moveDuration = 120L
                    changeDuration = 120L
                    removeDuration = 100L
                }
                doOnLayout {
                    addOnScrollListener(
                        PushUpScrollListener(
                            binding.up, it, R.id.student_name, R.id.student_course
                        )
                    )
                }
            }
        }

        binding.up.setOnClickListener {
            requireActivity().finish()
        }

//        binding.imageProfile.setOnClickListener {
//            pickImage()
//        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_SELECT_PICTURE)
    }

    private fun onImagePicked(uri: Uri) {
        setupViewModel.setSelectedImage(uri)
//        GlideApp.with(requireContext())
//            .load(uri)
//            .fallback(R.mipmap.ic_unes_large_image_512)
//            .placeholder(R.mipmap.ic_unes_large_image_512)
//            .circleCrop()
//            .transition(DrawableTransitionOptions.withCrossFade())
//            .into(binding.user)

        setupViewModel.uploadImageToStorage()
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
        fun newInstance(profileId: Long): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf(EXTRA_PROFILE_ID to profileId)
            }
        }
    }
}