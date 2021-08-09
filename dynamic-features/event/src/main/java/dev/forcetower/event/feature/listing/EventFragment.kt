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

package dev.forcetower.event.feature.listing

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.core.view.forEach
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.core.injection.dependencies.EventModuleDependencies
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.event.R
import dev.forcetower.event.core.injection.DaggerEventComponent
import dev.forcetower.event.databinding.FragmentEventBinding
import dev.forcetower.event.feature.details.EventDetailsActivity
import javax.inject.Inject

@Keep
class EventFragment : UFragment() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    private lateinit var binding: FragmentEventBinding
    private lateinit var adapter: EventAdapter
    private val viewModel: EventViewModel by viewModels { factory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerEventComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    EventModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEventBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = EventAdapter(viewModel)
        binding.recyclerEvents.adapter = adapter
        viewModel.events.observe(
            viewLifecycleOwner,
            Observer {
                adapter.submitList(it.sortedBy { value -> value.startDate })
                binding.isEmpty = it.isEmpty()
            }
        )

        viewModel.onEventClicked.observe(
            viewLifecycleOwner,
            EventObserver {
                val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                    putExtra("eventId", it.id)
                }
                val container = findEventShot(binding.recyclerEvents, it.id)
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(),
                    Pair.create(container, getString(R.string.transition_event_image)),
                    Pair.create(container, getString(R.string.transition_detail_background))
                )
                startActivity(intent, options.toBundle())
            }
        )

        binding.btnCreateEvent.setOnClickListener {
            val directions = EventFragmentDirections.actionEventsToCreateEvent()
            findNavController().navigate(directions)
        }
    }

    private fun findEventShot(entities: ViewGroup, id: Long): View {
        entities.forEach {
            if (it.getTag(R.id.tag_event_id) == id) {
                return it.findViewById(R.id.image)
            }
        }
        return entities
    }
}
