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

package com.forcetower.uefs.feature.siecomp.session

import android.app.ActivityOptions
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NavUtils
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentEventSessionDetailsBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.siecomp.speaker.EventSpeakerActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SessionDetailsFragment : UFragment() {
    private val viewModel: SIECOMPSessionViewModel by activityViewModels()
    private lateinit var binding: FragmentEventSessionDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEventSessionDetailsBinding.inflate(inflater, container, false).apply {
            up.setOnClickListener {
                NavUtils.navigateUpFromSameTask(requireActivity())
            }
        }

        val detailsAdapter = SessionDetailAdapter(this, viewModel)
        binding.sessionDetailRecyclerView.run {
            adapter = detailsAdapter
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
                        R.id.session_detail_title,
                        R.id.detail_image
                    )
                )
            }
        }

        viewModel.speakers.observe(
            viewLifecycleOwner,
            Observer {
                detailsAdapter.speakers = it ?: emptyList()
            }
        )

        viewModel.navigateToSpeakerAction.observe(
            viewLifecycleOwner,
            EventObserver { speakerId ->
                requireActivity().run {
                    val sharedElement = findSpeakerHeadshot(binding.sessionDetailRecyclerView, speakerId)
                    val option = ActivityOptions.makeSceneTransitionAnimation(this, sharedElement, getString(R.string.speaker_headshot_transition))
                    startActivity(EventSpeakerActivity.startIntent(this, speakerId), option.toBundle())
                }
            }
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        viewModel.setSessionId(requireNotNull(arguments).getLong(EXTRA_SESSION_ID))
    }

    override fun onStop() {
        super.onStop()
        viewModel.setSessionId(null)
    }

    private fun findSpeakerHeadshot(speakers: ViewGroup, speakerId: Long): View {
        speakers.forEach {
            if (it.getTag(R.id.tag_speaker_id) == speakerId) {
                return it.findViewById(R.id.speaker_item_headshot)
            }
        }
        Timber.e("Could not find view for speaker id $speakerId")
        return speakers
    }

    companion object {
        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun newInstance(id: Long): SessionDetailsFragment {
            val bundle = Bundle().apply { putLong(EXTRA_SESSION_ID, id) }
            return SessionDetailsFragment().apply { arguments = bundle }
        }
    }
}
