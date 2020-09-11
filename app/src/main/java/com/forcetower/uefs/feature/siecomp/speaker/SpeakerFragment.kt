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

package com.forcetower.uefs.feature.siecomp.speaker

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.app.NavUtils
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentEventSpeakerBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.forcetower.uefs.feature.shared.extensions.postponeEnterTransition
import com.forcetower.uefs.feature.siecomp.editor.CreateSpeakerFragment
import com.forcetower.uefs.feature.siecomp.session.PushUpScrollListener
import com.forcetower.uefs.feature.siecomp.speaker.EventSpeakerActivity.Companion.SPEAKER_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpeakerFragment : UFragment() {
    private val speakerViewModel: SIECOMPSpeakerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        speakerViewModel.setSpeakerId(requireNotNull(arguments).getLong(SPEAKER_ID))
        activity?.postponeEnterTransition(500L)

        val binding = FragmentEventSpeakerBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@SpeakerFragment
        }

        speakerViewModel.hasProfileImage.observe(
            viewLifecycleOwner,
            Observer {
                if (!it) {
                    activity?.startPostponedEnterTransition()
                }
            }
        )

        val headLoadListener = object : ImageLoadListener {
            override fun onImageLoaded(drawable: Drawable) { activity?.startPostponedEnterTransition() }
            override fun onImageLoadFailed() { activity?.startPostponedEnterTransition() }
        }

        val speakerAdapter = SpeakerAdapter(this, speakerViewModel, headLoadListener)
        binding.speakerDetailRecyclerView.run {
            adapter = speakerAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
            doOnLayout {
                addOnScrollListener(
                    PushUpScrollListener(binding.up, it, R.id.speaker_name, R.id.speaker_grid_image)
                )
            }
        }
        binding.up.setOnClickListener {
            NavUtils.navigateUpFromSameTask(requireActivity())
        }

        speakerViewModel.access.observe(
            viewLifecycleOwner,
            Observer {
                binding.editFloat.visibility = if (it != null) {
                    VISIBLE
                } else {
                    GONE
                }
            }
        )

        binding.editFloat.setOnClickListener {
            parentFragmentManager.inTransaction {
                replace(
                    R.id.speaker_container,
                    CreateSpeakerFragment().apply {
                        arguments = this@SpeakerFragment.arguments
                    }
                )
                addToBackStack(null)
            }
        }

        return binding.root
    }

    companion object {
        fun newInstance(speakerId: Long): SpeakerFragment {
            return SpeakerFragment().apply {
                arguments = bundleOf(SPEAKER_ID to speakerId)
            }
        }
    }
}
