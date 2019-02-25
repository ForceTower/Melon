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

package com.forcetower.uefs.feature.siecomp.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEventEdtCrtSpeakerBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.siecomp.speaker.EventSpeakerActivity.Companion.SPEAKER_ID
import com.forcetower.uefs.feature.siecomp.speaker.SIECOMPSpeakerViewModel
import javax.inject.Inject

class CreateSpeakerFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var binding: FragmentEventEdtCrtSpeakerBinding
    private lateinit var viewModel: SIECOMPSpeakerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        return FragmentEventEdtCrtSpeakerBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uuid = arguments?.getString(SPEAKER_UUID) ?: ""
        val id = arguments?.getLong(SPEAKER_ID)
        viewModel.setSpeakerId(id)
        viewModel.speaker.observe(this, Observer {
            binding.speaker = it
            binding.executePendingBindings()
        })

        binding.save.setOnClickListener {
            val name = binding.name.text.toString()
            val image = binding.image.text.toString()
            val lab = binding.lab.text.toString()
            val resume = binding.resume.text.toString()
            val url = binding.url.text.toString()
            val github = binding.github.text.toString()
            val speaker = Speaker(id ?: 0, name, image, lab, resume, url, github, uuid)
            viewModel.sendSpeaker(speaker, id == null)
        }
    }

    companion object {
        const val SPEAKER_UUID = "speaker_uuid"
    }
}