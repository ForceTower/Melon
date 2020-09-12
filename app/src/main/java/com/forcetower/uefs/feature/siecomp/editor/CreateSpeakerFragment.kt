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

package com.forcetower.uefs.feature.siecomp.editor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.model.siecomp.Speaker
import com.forcetower.uefs.databinding.FragmentEventEdtCrtSpeakerBinding
import com.forcetower.uefs.feature.siecomp.speaker.SIECOMPSpeakerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateSpeakerFragment : ImagePickerFragment() {
    private val viewModel: SIECOMPSpeakerViewModel by viewModels()
    private lateinit var binding: FragmentEventEdtCrtSpeakerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEventEdtCrtSpeakerBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uuid = arguments?.getString(SPEAKER_UUID) ?: ""
        val id = arguments?.getLong(SPEAKER_ID)
        viewModel.setSpeakerId(id)
        viewModel.speaker.observe(
            viewLifecycleOwner,
            Observer {
                binding.speaker = it
                binding.executePendingBindings()
            }
        )

        binding.imageUserPicture.setOnClickListener {
            pickImage()
        }

        binding.save.setOnClickListener {
            val name = binding.name.text.toString()
            val lab = binding.lab.text.toString()
            val resume = binding.resume.text.toString()
            val url = binding.url.text.toString()
            val github = binding.github.text.toString()
            val speaker = Speaker(id ?: 0, name, viewModel.uriString, lab, resume, url, github, uuid)
            viewModel.sendSpeaker(speaker, id == null)
        }
    }

    override fun onImagePicked(uri: Uri) {
        viewModel.uriString = uri.toString()
    }

    companion object {
        const val SPEAKER_UUID = "speaker_uuid"
        const val SPEAKER_ID = "speaker_id"
    }
}
