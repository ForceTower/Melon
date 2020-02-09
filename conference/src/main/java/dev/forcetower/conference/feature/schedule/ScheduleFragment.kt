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

package dev.forcetower.conference.feature.schedule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.uefs.feature.shared.UDynamicFragment
import com.forcetower.uefs.feature.shared.clearDecorations
import com.forcetower.uefs.feature.shared.executeBindingsAfter
import dev.forcetower.conference.core.injection.DaggerConferenceComponent
import dev.forcetower.conference.core.model.domain.ConferenceDayIndexed
import dev.forcetower.conference.core.model.domain.DayIndicator
import dev.forcetower.conference.core.model.domain.ScheduleUiData
import dev.forcetower.conference.core.model.persistence.ConferenceDay
import dev.forcetower.conference.core.model.persistence.Session
import dev.forcetower.conference.databinding.FragmentScheduleBinding
import org.threeten.bp.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

class ScheduleFragment : UDynamicFragment() {
    @Inject
    lateinit var factory: BaseViewModelFactory
    private lateinit var binding: FragmentScheduleBinding
    private lateinit var dayAdapter: DayAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val viewModel by viewModels<ScheduleViewModel> { factory }

    private var cachedBubbleRange: IntRange? = null
    private lateinit var dayIndexed: ConferenceDayIndexed

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerConferenceComponent.builder().appComponent(component).build().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            dayAdapter = DayAdapter(viewModel, this)
            binding.dayIndicators.adapter = dayAdapter
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildDayIndicators(days)
        onScheduleUpdate(schedule)
    }

    private fun onScheduleUpdate(schedule: ScheduleUiData) {
        val list = schedule.sessions
        val indexed = schedule.indexed
        dayIndexed = indexed

        cachedBubbleRange = null
        if (indexed.days.isEmpty()) {
            cachedBubbleRange = -1..-1
            buildDayIndicators(indexed.days)
        }
        scheduleAdapter.submitList(list)
        binding.recyclerSchedule.run {
            clearDecorations()
            if (list.isNotEmpty()) {
//                addItemDecoration(
//                    ScheduleTimeHeadersDecoration(
//                        context, list
//                    )
//                )
//                addItemDecoration(
//                    DaySeparatorItemDecoration(
//                        context, indexed
//                    )
//                )
            }
        }

        binding.executeBindingsAfter {
            isEmpty = list.isEmpty()
        }
    }

    private fun buildDayIndicators(days: List<ConferenceDay>) {
//        val bubbleRange = cachedBubbleRange ?: return
        val mapped = days.mapIndexed { index: Int, day: ConferenceDay ->
            DayIndicator(day = day, checked = false)
        }
        dayAdapter.submitList(mapped)
    }

    companion object {
        val days = listOf(
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now(), ZonedDateTime.now().plusHours(3), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(1).plusHours(3), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(2), ZonedDateTime.now().plusDays(2).plusHours(3), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(3), ZonedDateTime.now().plusDays(3).plusHours(3), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(4), ZonedDateTime.now().plusDays(4).plusHours(3), 1)
        )

        val sessions = (0..20).map {
            Session(
                UUID.randomUUID().toString(),
                ZonedDateTime.now().plusHours(3),
                ZonedDateTime.now().plusHours(4),
                "A different session",
                "I put nintendo switch over people doing dumb stuff",
                "Center Auditorium",
                "https://www.hoohle.com",
                1,
                days[it % days.size].id
            )
        }

        private fun buildConferenceDayIndexer(sessions: List<Session>): ConferenceDayIndexed {
            val sorted = sessions.sortedBy { it.startTime }
            val mapping = days
                .associateWith { day ->
                    sorted.indexOfFirst {
                        day.contains(it)
                    }
                }
                .filterValues { it >= 0 }

            return ConferenceDayIndexed(mapping)
        }

        val schedule = ScheduleUiData(sessions, buildConferenceDayIndexer(sessions))
    }
}