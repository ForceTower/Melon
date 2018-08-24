/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.operation.semester

import com.forcetower.sagres.database.model.SSemester
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status

class SemesterCallback(status: Status) : BaseCallback<SemesterCallback>(status) {
    private var semesters: List<SSemester> = ArrayList()

    fun semesters(semesters: List<SSemester>): SemesterCallback {
        this.semesters = semesters
        return this
    }

    fun getSemesters(): List<SSemester> = semesters
}