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

package com.forcetower.uefs.feature.schedule

import android.view.View
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.database.accessors.GroupWithClass
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.uefs.core.storage.repository.SagresSyncRepository
import com.forcetower.uefs.easter.twofoureight.Game2048Activity
import com.forcetower.uefs.feature.disciplines.disciplinedetail.DisciplineDetailsActivity
import javax.inject.Inject

class ScheduleViewModel @Inject constructor(
    private val database: UDatabase,
    private val sagresSyncRepository: SagresSyncRepository
) : ViewModel(), ScheduleActions {

    val scheduleSrc by lazy { database.classLocationDao().getCurrentSchedule() }

    override fun onLongClick(view: View): Boolean {
        Game2048Activity.startActivity(view.context)
        return false
    }

    override fun onClick(view: View, group: GroupWithClass) {
        val context = view.context
        val intent = DisciplineDetailsActivity.startIntent(context, group.clazz().clazz.uid, group.group.uid)
        context.startActivity(intent)
    }

    override fun onLocationClick(view: View, location: LocationWithGroup) {
        val group = location.singleGroup()
        if (group.singleClass().clazz.scheduleOnly) return
        val context = view.context
        val intent = DisciplineDetailsActivity.startIntent(context, group.clazz().clazz.uid, group.group.uid)
        context.startActivity(intent)
    }

    override fun refreshData() {
        sagresSyncRepository.asyncSync()
    }
}