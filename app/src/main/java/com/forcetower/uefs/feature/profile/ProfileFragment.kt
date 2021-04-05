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

package com.forcetower.uefs.feature.profile

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentProfileBinding
import com.forcetower.uefs.feature.profile.ProfileActivity.Companion.EXTRA_STUDENT_ID
import com.forcetower.uefs.feature.profile.ProfileActivity.Companion.EXTRA_USER_ID
import com.forcetower.uefs.feature.setup.SetupViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.forcetower.uefs.feature.shared.extensions.postponeEnterTransition
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.siecomp.session.PushUpScrollListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : UFragment() {
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseStorage: FirebaseStorage

    private val viewModel: ProfileViewModel by viewModels()
    private val setupViewModel: SetupViewModel by viewModels()
    private lateinit var binding: FragmentProfileBinding
    private lateinit var adapter: ProfileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.postponeEnterTransition(500L)
        val userId = requireNotNull(arguments).getLong(EXTRA_USER_ID, 0)
        check(userId != 0L) { "Well.. That happened" }
        viewModel.setUserId(userId)
        viewModel.setProfileId(requireNotNull(arguments).getLong(EXTRA_STUDENT_ID, 0))
        viewModel.profile.observe(
            viewLifecycleOwner,
            Observer {
                it ?: return@Observer
                if (it.imageUrl == null) {
                    activity?.startPostponedEnterTransition()
                }
                if (it.me) {
                    binding.writeStatement.hide()
                } else {
                    binding.writeStatement.show()
                }
            }
        )

        val headLoadListener = object : ImageLoadListener {
            override fun onImageLoaded(drawable: Drawable) { activity?.startPostponedEnterTransition() }
            override fun onImageLoadFailed() { activity?.startPostponedEnterTransition() }
        }

        adapter = ProfileAdapter(viewModel, this, headLoadListener, viewModel)
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
                            binding.up,
                            it,
                            R.id.student_name,
                            R.id.student_course
                        )
                    )
                }
            }
        }

        viewModel.statements.observe(
            viewLifecycleOwner,
            Observer { statements ->
                adapter.statements = statements.sortedByDescending { it.createdAt }
            }
        )

        binding.up.setOnClickListener {
            requireActivity().finishAfterTransition()
        }

        binding.writeStatement.setOnClickListener {
            val profileId = requireNotNull(arguments).getLong(EXTRA_STUDENT_ID, 0)
            val userId = requireNotNull(arguments).getLong(EXTRA_USER_ID, 0)
            parentFragmentManager.inTransaction {
                val fragment = WriteStatementFragment().apply {
                    arguments = bundleOf(
                        EXTRA_STUDENT_ID to profileId,
                        EXTRA_USER_ID to userId
                    )
                }
                replace(R.id.fragment_container, fragment, "write_statement")
                addToBackStack("home")
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_SELECT_PICTURE)
    }

    private fun onImagePicked(uri: Uri) {
        setupViewModel.setSelectedImage(uri)
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
        fun newInstance(profileId: Long, userId: Long): ProfileFragment {
            return ProfileFragment().apply {
                arguments = bundleOf(
                    EXTRA_STUDENT_ID to profileId,
                    EXTRA_USER_ID to userId
                )
            }
        }
    }
}
