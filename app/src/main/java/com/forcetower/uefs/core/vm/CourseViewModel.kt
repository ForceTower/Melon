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

package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.task.UCaseResult
import com.forcetower.uefs.core.task.data
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.unes.usecases.courses.LoadCoursesUseCase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val loadCoursesUseCase: LoadCoursesUseCase
) : ViewModel() {

    fun getCourses(): LiveData<List<Course>> {
        return loadCoursesUseCase(Unit)
            .filter { it is UCaseResult.Success }
            .map { it.data }
            .filterNotNull()
            .asLiveData()
    }
}
