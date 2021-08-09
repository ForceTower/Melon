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

package com.forcetower.uefs.feature.adventure

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.databinding.DialogAdventureSignInBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.UGameActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AdventureSignInDialog : RoundedDialog() {
    private val viewModel: AdventureViewModel by activityViewModels()
    private lateinit var binding: DialogAdventureSignInBinding
    private var activity: UGameActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as? UGameActivity
        activity ?: Timber.e("Adventure Fragment must be attached to a UGameActivity for it to work")
    }

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogAdventureSignInBinding.inflate(inflater, container, false).apply {
            interactor = viewModel
            lifecycleOwner = this@AdventureSignInDialog
            executePendingBindings()
        }.also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start.observe(
            viewLifecycleOwner,
            EventObserver {
                dismiss()
                activity?.signIn()
            }
        )
        binding.btnCancel.setOnClickListener {
            dismiss()
            findNavController().popBackStack()
        }
    }
}
