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

package com.forcetower.uefs.feature.evaluation

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentEvaluationPresentationBinding
import com.forcetower.uefs.feature.information.InformationDialog
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PresentationFragment : UFragment() {
    @Inject
    lateinit var preferences: SharedPreferences
    private lateinit var binding: FragmentEvaluationPresentationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEvaluationPresentationBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnConnect.setOnClickListener { next() }
            btnQuestion.setOnClickListener { onQuestion() }
        }.root
    }

    fun next() {
        val direction = PresentationFragmentDirections.actionPresentationToHome()
        preferences.edit().putBoolean("evaluation_presentation_shown", true).apply()
        findNavController().navigate(direction)
    }

    private fun onQuestion() {
        val dialog = InformationDialog()
        dialog.title = getString(R.string.evaluation_what_is)
        dialog.description = getString(R.string.evaluation_what_is_description)
        dialog.show(childFragmentManager, "what_is_evaluation")
    }
}
