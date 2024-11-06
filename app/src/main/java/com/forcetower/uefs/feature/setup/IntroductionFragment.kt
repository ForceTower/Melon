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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.repository.SyncFrequencyRepository
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.FragmentSetupIntroductionBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IntroductionFragment : UFragment() {
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
        binding.imageUserImage.setOnClickListener {
            pickImage()
        }

        binding.btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_introduction_to_configuration)
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
        Glide.with(requireContext())
            .load(uri)
            .fallback(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .placeholder(com.forcetower.core.R.mipmap.ic_unes_large_image_512)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageUserImage)
    }
}
