/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.feature.barrildeboa.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.architecture.service.discipline.DisciplineDetailsLoaderService
import com.forcetower.uefs.databinding.DialogHourglassExplanationBinding
import com.forcetower.uefs.databinding.FragmentHourglassLandingBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.UFragment

class LandingFragment : UFragment() {
    private lateinit var binding: FragmentHourglassLandingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentHourglassLandingBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            explainButton.setOnClickListener {
                val dialog = ExplainHourglassDialog()
                dialog.show(childFragmentManager, "explain_hourglass_dialog")
            }
            confirmButton.setOnClickListener {
                DisciplineDetailsLoaderService.startService(requireContext())
                activity?.finish()
            }
        }
    }

    internal class ExplainHourglassDialog : RoundedDialog() {
        override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return DialogHourglassExplanationBinding.inflate(inflater, container, false).apply {
                btnOk.setOnClickListener { dismiss() }
            }.root
        }
    }
}