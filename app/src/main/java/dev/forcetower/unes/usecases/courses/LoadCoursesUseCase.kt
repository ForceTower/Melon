/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.unes.usecases.courses

import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.storage.repository.CourseRepository
import com.forcetower.uefs.core.task.FlowUseCase
import com.forcetower.uefs.core.task.UCaseResult
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@Reusable
class LoadCoursesUseCase @Inject constructor(
    private val repository: CourseRepository
) : FlowUseCase<Unit, List<Course>>() {
    override fun execute(parameters: Unit): Flow<UCaseResult<List<Course>>> {
        return repository.getCourses()
    }
}
