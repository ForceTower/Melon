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

package com.forcetower.uefs.core.storage.repository

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.disciplinedetails.DisciplineDetailsCallback
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.resource.LoadDisciplineDetailsResource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisciplineLoaderRepository @Inject constructor(
    private val database: UDatabase,
    private val executors: AppExecutors
) {

    /**
     * This is a funny function
     * It will load the details of any discipline that matches the requirements, if all the parameters
     * are null, it will load the details of every single discipline.
     *
     * This function may take several minutes to complete, it's better to be called within a service
     * or something that shows to the user that this long operation is running.
     *
     * Funny fact: It only saves the data once it has completed downloading everything, a network
     * connection fail during the process will generate a catastrophic fail.
     *
     * Note to myself: save the data when it is ready, don't wait until everything completes
     *
     * Second note: add a "don't load this discipline", kind of a blacklist so the process can load
     * it faster
     *
     * Third note: add a "simple load" and a "complex load". Simple load will only fetch the teacher
     * name, which is the common use for this function.
     */
    @MainThread
    fun loadDisciplineDetails(semester: String? = null, code: String? = null, group: String? = null): LiveData<DisciplineDetailsCallback> {
        return object: LoadDisciplineDetailsResource(executors, semester, code, group) {
            @WorkerThread
            override fun saveResults(groups: List<SDisciplineGroup>) {
                database.classGroupDao().defineGroups(groups)
            }
        }.asLiveData()
    }
}