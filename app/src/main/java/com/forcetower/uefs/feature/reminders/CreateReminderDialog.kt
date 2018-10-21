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

package com.forcetower.uefs.feature.reminders

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogCreateReminderBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.shared.provideViewModel
import java.util.Calendar
import javax.inject.Inject

class CreateReminderDialog: RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: DialogCreateReminderBinding
    private lateinit var viewModel: RemindersViewModel

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
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
        val description = binding.textInputDescription.text?.toString()

        if (title == null) {
            binding.textInputTitle.error = getString(R.string.reminder_title_empty)
            binding.textInputTitle.requestFocus()
            return
        }

        viewModel.createReminder(title, description)
        dismiss()
    }

    private fun openCalendar() {
        val calendar = Calendar.getInstance()
        val date = calendar.get(Calendar.DAY_OF_MONTH)
        val mont = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val picker = DatePickerDialog(requireContext(), DatePickerDialog.OnDateSetListener { _, y, m, d ->
            val next = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
            }.timeInMillis
            viewModel.currentDeadline = next
            binding.btnDeadline.iconTint = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_accent))
        }, year, mont, date)
        picker.show()
    }
}