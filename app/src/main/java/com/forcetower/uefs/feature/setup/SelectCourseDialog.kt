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

package com.forcetower.uefs.feature.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.vm.CourseViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.DialogSelectCourseBinding
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import javax.inject.Inject

class SelectCourseDialog: RoundedDialog(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: CourseViewModel
    private lateinit var binding: DialogSelectCourseBinding
    private var courses: Array<Course>? = null
    private var callback: CourseSelectionCallback? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return DialogSelectCourseBinding.inflate(inflater, container, false).also {
            binding = it
            it.btnCancel.setOnClickListener {_ -> dismiss()}
            it.btnOk.setOnClickListener {_ -> select() }
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.courses.observe(this, Observer {
            if (it.data != null) {
                populateCourses(it.data)
            }
        })
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