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

package com.forcetower.uefs.feature.reminders

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.DialogCreateReminderBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.inflate
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class CreateReminderDialog : RoundedDialog() {
    private val viewModel: RemindersViewModel by viewModels()
    private lateinit var binding: DialogCreateReminderBinding

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = inflater.inflate(R.layout.dialog_create_reminder)
        binding.apply {
            btnOk.setOnClickListener { createReminder() }
            btnDeadline.setOnClickListener { openCalendar() }
            btnCancel.setOnClickListener { dismiss() }
        }
        return binding.root
    }

    private fun createReminder() {
        val title = binding.textInputTitle.text?.toString()
        var description = binding.textInputDescription.text?.toString()

        if (title == null || title.isBlank()) {
            binding.textInputTitle.error = getString(R.string.reminder_title_empty)
            binding.textInputTitle.requestFocus()
            return
        }

        if (description != null && description.isBlank())
            description = null

        viewModel.createReminder(title, description)
        dismiss()
    }

    private fun openCalendar() {
        val calendar = Calendar.getInstance()

        if (viewModel.currentDeadline != null)
            calendar.timeInMillis = viewModel.currentDeadline!!

        val color = ViewUtils.attributeColorUtils(requireContext(), R.attr.colorPrimary)

        val picker = DatePickerDialog.newInstance(
            { _, y, m, d ->
                val next = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }.timeInMillis
                viewModel.currentDeadline = next
                binding.btnDeadline.iconTint = ColorStateList.valueOf(color)
            },
            calendar
        )
        picker.version = DatePickerDialog.Version.VERSION_2

        picker.accentColor = color
        picker.setOkColor(color)

        val theme = requireContext().theme
        val darkThemeValue = TypedValue()
        theme.resolveAttribute(R.attr.lightStatusBar, darkThemeValue, true)
        picker.isThemeDark = darkThemeValue.data == 0

        val colorOnSurfaceLight = TypedValue()
        theme.resolveAttribute(R.attr.colorOnSurfaceLight, colorOnSurfaceLight, true)
        picker.setCancelColor(colorOnSurfaceLight.data)

        picker.show(childFragmentManager, "date_picker_dialog")
    }
}
