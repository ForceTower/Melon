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

package com.forcetower.uefs.feature.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.databinding.DialogSelectCourseBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectCourseDialog : RoundedDialog() {
    private val viewModel by activityViewModels<CourseViewModel>()
    private lateinit var binding: DialogSelectCourseBinding
    private var courses: Array<Course>? = null
    private var callback: CourseSelectionCallback? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val hide = arguments?.getBoolean("hide_description") ?: false
        return DialogSelectCourseBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener { dismiss() }
            it.btnOk.setOnClickListener { select() }
            it.labelCourseInformation.visibility = if (hide) View.GONE else View.VISIBLE
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getCourses().observe(
            viewLifecycleOwner,
            {
                if (it.isNotEmpty()) {
                    populateCourses(it)
                }
            }
        )
    }

    private fun populateCourses(data: List<Course>) {
        courses = data.toTypedArray()
        val strings = data.map { it.name }.toTypedArray()
        binding.pickerCourse.minValue = 1
        binding.pickerCourse.maxValue = strings.size
        binding.pickerCourse.displayedValues = strings
    }

    private fun select() {
        val not = courses ?: emptyArray()
        if (not.isNotEmpty()) {
            callback?.onSelected(not[binding.pickerCourse.value - 1])
            dismiss()
        }
    }

    fun setCallback(callback: CourseSelectionCallback) {
        this.callback = callback
    }
}

interface CourseSelectionCallback {
    fun onSelected(course: Course)
}
