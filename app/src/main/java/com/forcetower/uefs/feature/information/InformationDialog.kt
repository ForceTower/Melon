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

package com.forcetower.uefs.feature.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.DialogInformationBaseBinding
import com.forcetower.uefs.feature.shared.RoundedDialog

class InformationDialog : RoundedDialog() {
    private lateinit var binding: DialogInformationBaseBinding
    lateinit var title: String
    lateinit var description: String
    @Suppress("MemberVisibilityCanBePrivate")
    var buttonText: String? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogInformationBaseBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnOk.setOnClickListener { dismiss() }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title = title
        binding.description = description
        binding.buttonText = buttonText ?: getString(R.string.got_it)
    }
}
