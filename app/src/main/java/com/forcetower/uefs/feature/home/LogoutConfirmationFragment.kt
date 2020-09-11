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

package com.forcetower.uefs.feature.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentLogoutConfirmationBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LogoutConfirmationFragment : BottomSheetDialogFragment() {
    private val viewModel: HomeViewModel by activityViewModels()
    private lateinit var binding: FragmentLogoutConfirmationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentLogoutConfirmationBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnCancel.setOnClickListener { dismiss() }
            btnConfirm.setOnClickListener { viewModel.logout(); dismiss() }
        }.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val created = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        created.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog

            val bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                Timber.e("Bottom sheet dialog is null.. Theme will not apply")
            }
        }
        return created
    }
}
