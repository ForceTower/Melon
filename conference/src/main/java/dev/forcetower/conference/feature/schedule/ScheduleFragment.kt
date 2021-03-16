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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.injection.dependencies.ConferenceModuleDependencies
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.clearDecorations
import com.forcetower.uefs.feature.shared.executeBindingsAfter
import com.forcetower.uefs.widget.BubbleDecoration
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.conference.core.injection.DaggerConferenceComponent
import dev.forcetower.conference.core.model.domain.ConferenceDayIndexed
import dev.forcetower.conference.core.model.domain.DayIndicator
import dev.forcetower.conference.core.model.domain.ScheduleUiData
import dev.forcetower.conference.core.model.persistence.ConferenceDay
import dev.forcetower.conference.core.model.persistence.Session
import dev.forcetower.conference.core.ui.schedule.DaySeparatorItemDecoration
import dev.forcetower.conference.core.ui.schedule.ScheduleTimeHeadersDecoration
import dev.forcetower.conference.databinding.FragmentConferenceScheduleBinding
import java.time.ZonedDateTime
import java.util.UUID

class ScheduleFragment : UFragment() {
    private lateinit var binding: FragmentConferenceScheduleBinding
    private lateinit var dayAdapter: DayAdapter
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var dayIndicatorItemDecoration: BubbleDecoration
    private val viewModel by viewModels<ScheduleViewModel>()

    private var cachedBubbleRange: IntRange? = null
    private lateinit var dayIndexed: ConferenceDayIndexed

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerConferenceComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ConferenceModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dayAdapter = DayAdapter(viewModel, this)
        scheduleAdapter = ScheduleAdapter(viewModel, this)

        FragmentConferenceScheduleBinding.inflate(inflater, container, false).also {
            binding = it
            binding.dayIndicators.adapter = dayAdapter
            binding.recyclerSchedule.adapter = scheduleAdapter
        }

        dayIndicatorItemDecoration = BubbleDecoration(binding.root.context)
        binding.dayIndicators.addItemDecoration(dayIndicatorItemDecoration)

        binding.recyclerSchedule.run {
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }

            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recycler: RecyclerView, dx: Int, dy: Int) {
                        onScheduleScrolled()
                    }
                }
            )
        }

        return binding.root
    }

    private fun onScheduleScrolled() {
        val manager = (binding.recyclerSchedule.layoutManager) as LinearLayoutManager
        val first = manager.findFirstVisibleItemPosition()
        val last = manager.findLastVisibleItemPosition()
        if (first < 0 || last < 0) {
            return
        }

        val firstDay = dayIndexed.dayForPosition(first) ?: return
        val lastDay = dayIndexed.dayForPosition(last) ?: return
        val highlightRange = dayIndexed.days.indexOf(firstDay)..dayIndexed.days.indexOf(lastDay)
        if (highlightRange != cachedBubbleRange) {
            cachedBubbleRange = highlightRange
            buildDayIndicators(days)
        }
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
                addItemDecoration(
                    ScheduleTimeHeadersDecoration(
                        context,
                        list
                    )
                )
                addItemDecoration(
                    DaySeparatorItemDecoration(
                        context,
                        indexed
                    )
                )
            }
        }

        binding.dayIndicators.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    dayIndicatorItemDecoration.userScrolled = true
                }
            }
        )

        binding.executeBindingsAfter {
            isEmpty = list.isEmpty()
        }
    }

    private fun buildDayIndicators(days: List<ConferenceDay>) {
        val bubbleRange = cachedBubbleRange ?: return
        val mapped = days.mapIndexed { index: Int, day: ConferenceDay ->
            DayIndicator(day = day, checked = index in bubbleRange)
        }
        dayAdapter.submitList(mapped)
        dayIndicatorItemDecoration.bubbleRange = bubbleRange
    }

    companion object {
        val days = listOf(
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now(), ZonedDateTime.now().plusHours(8), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(1).plusHours(8), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(2), ZonedDateTime.now().plusDays(2).plusHours(8), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(3), ZonedDateTime.now().plusDays(3).plusHours(8), 1),
            ConferenceDay(UUID.randomUUID().toString(), ZonedDateTime.now().plusDays(4), ZonedDateTime.now().plusDays(4).plusHours(8), 1)
        )

        val sessions = (0..60).map {
            val day = days[it % days.size]
            val sorted = (3..10).random().toLong()
            Session(
                UUID.randomUUID().toString(),
                day.start.plusHours(sorted),
                day.start.plusHours(sorted + 2),
                "A different session",
                "I put nintendo switch over people doing dumb stuff",
                "Center Auditorium",
                "https://www.hoohle.com",
                1,
                day.id
            )
        }.sortedBy { it.startTime }

        private fun buildConferenceDayIndexer(sessions: List<Session>): ConferenceDayIndexed {
            val mapping = days
                .associateWith { day ->
                    sessions.indexOfFirst {
                        day.contains(it)
                    }
                }
                .filterValues { it >= 0 }

            return ConferenceDayIndexed(mapping)
        }

        val schedule = ScheduleUiData(sessions, buildConferenceDayIndexer(sessions))
    }
}
