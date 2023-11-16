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

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : UFragment() {
    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var firebaseStorage: FirebaseStorage

    private val pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
        onContentSelected(it)
    }

    private val cropImage = registerForActivityResult(CropImageContract()) {
        onCropResults(it)
    }

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
            viewLifecycleOwner
        ) { statements ->
            adapter.statements = statements.sortedByDescending { it.createdAt }
        }

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

    fun pickImage() {
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
        setupViewModel.setSelectedImage(uri)
        setupViewModel.uploadImageToStorage()
    }

    companion object {
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
