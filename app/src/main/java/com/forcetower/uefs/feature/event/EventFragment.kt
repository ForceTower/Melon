/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.service.Event
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentEventsBinding
import com.forcetower.uefs.feature.shared.TextViewFactory
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.ramotion.cardslider.CardSliderLayoutManager
import com.ramotion.cardslider.CardSnapHelper
import javax.inject.Inject

class EventFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: EventViewModel
    private lateinit var binding: FragmentEventsBinding
    private lateinit var manager: CardSliderLayoutManager

    private var currentPosition = 0
    private val items: MutableList<Event> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        FragmentEventsBinding.inflate(inflater, container, false).also {
            binding = it
            manager = binding.eventsRecycler.layoutManager as CardSliderLayoutManager
        }.apply {
            incToolbar.apply {
                textToolbarTitle.text = getString(R.string.label_events)
            }
        }

        initSwitchers()
        return binding.root
    }

    private fun initSwitchers() {
        binding.textEventName.setFactory(TextViewFactory(requireContext(), R.style.UTheme_EventTitleText))
        binding.textEventLocation.setFactory(TextViewFactory(requireContext(), R.style.UTheme_EventLocationText))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pagerAdapter = EventAdapter2()

        binding.eventsRecycler.apply {
            adapter = pagerAdapter
            setHasFixedSize(true)
        }

        binding.eventsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onActiveEventChange()
                }
            }
        })

        CardSnapHelper().attachToRecyclerView(binding.eventsRecycler)

        viewModel.events.observe(this, Observer {
            pagerAdapter.submitList(it)
            items.clear()
            items += it

            if (it.isEmpty()) currentPosition = RecyclerView.NO_POSITION
            else if (currentPosition >= it.size) currentPosition = it.size - 1

            if (currentPosition != RecyclerView.NO_POSITION) animateChange(currentPosition)
        })
    }

    fun onActiveEventChange() {
        val position = manager.activeCardPosition
        if (position == RecyclerView.NO_POSITION || position == currentPosition) return

        currentPosition = position
        animateChange(position)
    }

    private fun animateChange(position: Int) {
        val event = items[position]
        val top = R.anim.slide_in_top
        val bottom = R.anim.slide_out_bottom
        val ctx = requireContext()

        binding.run {
            textEventName.apply {
                setInAnimation(ctx, top)
                setOutAnimation(ctx, bottom)
                setText(event.name)
            }
            textEventLocation.apply {
                setInAnimation(ctx, top)
                setOutAnimation(ctx, bottom)
                setText(event.location)
            }
        }
    }
}