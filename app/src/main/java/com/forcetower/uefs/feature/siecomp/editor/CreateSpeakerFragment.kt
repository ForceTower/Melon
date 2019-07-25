/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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