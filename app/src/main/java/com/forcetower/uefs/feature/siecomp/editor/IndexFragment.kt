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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentEventEditorIndexBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import com.forcetower.uefs.feature.siecomp.SIECOMPEventViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IndexFragment : UFragment() {
    private val viewModel: SIECOMPEventViewModel by viewModels()
    private lateinit var binding: FragmentEventEditorIndexBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEventEditorIndexBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            editorAuth.setOnClickListener {
                val user = binding.username.text.toString()
                val pass = binding.password.text.toString()
                viewModel.loginToService(user, pass)
            }

            editorSpeaker.setOnClickListener {
                parentFragmentManager.inTransaction {
                    replace(R.id.fragment_container, CreateSpeakerFragment())
                    addToBackStack(null)
                }
            }
        }
    }
}
