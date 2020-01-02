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

package com.forcetower.uefs.feature.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.forcetower.uefs.R
import com.forcetower.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentSendFeedbackBinding
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

class SendFeedbackFragment : BottomSheetDialogFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    lateinit var viewModel: FeedbackViewModel
    lateinit var binding: FragmentSendFeedbackBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.UTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        viewModel = provideViewModel(factory)
        return FragmentSendFeedbackBinding.inflate(themedInflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            interactor = viewModel
            lifecycleOwner = this@SendFeedbackFragment
        }
        viewModel.textError.observe(viewLifecycleOwner, EventObserver {
            if (it == null) {
                binding.textFeedback.error = ""
            } else {
                binding.textFeedback.error = it
            }
        })

        viewModel.sendFeedback.observe(viewLifecycleOwner, EventObserver {
            if (it) {
                dismiss()
            }
        })
    }
}