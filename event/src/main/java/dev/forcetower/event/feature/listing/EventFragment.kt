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
import androidx.core.view.forEach
import androidx.fragment.app.viewModels
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.feature.shared.UDynamicFragment
import dev.forcetower.event.R
import dev.forcetower.event.core.injection.DaggerEventComponent
import dev.forcetower.event.databinding.FragmentEventBinding
import dev.forcetower.event.feature.details.EventDetailsActivity
import org.threeten.bp.ZonedDateTime
import java.util.UUID
import javax.inject.Inject

class EventFragment : UDynamicFragment() {
    @Inject
    lateinit var factory: BaseViewModelFactory
    private lateinit var binding: FragmentEventBinding
    private lateinit var adapter: EventAdapter
    private val viewModel: EventViewModel by viewModels { factory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerEventComponent.builder().appComponent(component).build().inject(this)
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
        adapter.submitList(events)

        viewModel.onEventClicked.observe(viewLifecycleOwner, EventObserver {
            val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
                putExtra("event_id", it.id)
            }
            val container = findEventShot(binding.recyclerEvents, it.id)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                requireActivity(),
                Pair.create(container, getString(R.string.transition_event_image)),
                Pair.create(container, getString(R.string.transition_detail_background))
            )
            startActivity(intent, options.toBundle())
        })
    }

    private fun findEventShot(entities: ViewGroup, id: String): View {
        entities.forEach {
            if (it.getTag(R.id.tag_event_id) == id) {
                return it.findViewById(R.id.image)
            }
        }
        return entities
    }

    companion object {
        val events = listOf(
            Event(
                UUID.randomUUID().toString(),
                "XXIII SIECOMP",
                "Muita coisa engraçada e gente legal",
                "https://images.even3.com.br/UPJVSvZBwbrcakjVHQLyiz90jHU=/1300x536/smart/even3.blob.core.windows.net/banner/BannerXXIISIECOMP.e34ffcd81b1d4a019044.jpg",
                "João Paulo",
                1,
                "Ele mesmo",
                ZonedDateTime.now().plusDays(3),
                ZonedDateTime.now().plusDays(3).plusHours(3),
                "Na sua casa",
                9.99,
                20,
                null,
                true,
                ZonedDateTime.now(),
                true,
                canModify = true
            )
        )
    }
}