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

package com.forcetower.uefs.architecture.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.repository.DisciplinesRepository
import com.forcetower.uefs.feature.shared.extensions.toTitleCase
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeClassWidget : AppWidgetProvider() {
    @Inject lateinit var repository: DisciplinesRepository

    private val job = SupervisorJob()
    private val widgetScope = CoroutineScope(Dispatchers.Main + job)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FirebaseAnalytics.getInstance(context).setUserProperty("enabled_widget_type", "primary")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val views = RemoteViews(context.packageName, R.layout.widget_class_home)

        widgetScope.launch {
            val current = repository.getCurrentClass()
            if (current != null) {
                val location = if (current.location.modulo == null && current.location.room == null) {
                    context.getString(R.string.widget_unknown_location)
                } else {
                    listOfNotNull(current.location.modulo, current.location.room).joinToString(" - ")
                }
                views.apply {
                    setTextViewText(R.id.discipline, current.groupData.classData.discipline.name.toTitleCase())
                    setTextViewText(R.id.teacher, current.groupData.group.teacher ?: context.getString(R.string.widget_unknown_teacher))
                    setTextViewText(R.id.location, location)
                    setTextViewText(R.id.time, "${current.location.startsAt} ~ ${current.location.endsAt}")
                }
            } else {
                views.apply {
                    setTextViewText(R.id.discipline, context.getString(R.string.widget_no_more_class_discipline))
                    setTextViewText(R.id.teacher, context.getString(R.string.widget_no_more_class_teacher))
                    setTextViewText(R.id.location, context.getString(R.string.widget_no_more_class_location))
                    setTextViewText(R.id.time, context.getString(R.string.widget_no_more_class_time))
                }
            }
            appWidgetIds.forEach {
                appWidgetManager.updateAppWidget(it, views)
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel("All widgets disabled")
    }
}
