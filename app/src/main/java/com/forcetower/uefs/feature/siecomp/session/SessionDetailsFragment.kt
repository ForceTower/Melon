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

package com.forcetower.uefs.feature.siecomp.session

import android.app.ActivityOptions
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NavUtils
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEventSessionDetailsBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.forcetower.uefs.feature.siecomp.speaker.EventSpeakerActivity
import timber.log.Timber
import javax.inject.Inject

class SessionDetailsFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: SIECOMPSessionViewModel
    private lateinit var binding: FragmentEventSessionDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
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
                        binding.up, it, R.id.session_detail_title, R.id.detail_image
                    )
                )
            }
        }

        viewModel.speakers.observe(this, Observer {
            detailsAdapter.speakers = it ?: emptyList()
        })

        viewModel.navigateToSpeakerAction.observe(this, EventObserver { speakerId ->
            requireActivity().run {
                val sharedElement = findSpeakerHeadshot(binding.sessionDetailRecyclerView, speakerId)
                val option = ActivityOptions.makeSceneTransitionAnimation(this, sharedElement, getString(R.string.speaker_headshot_transition))
                startActivity(EventSpeakerActivity.startIntent(this, speakerId), option.toBundle())
            }
        })

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