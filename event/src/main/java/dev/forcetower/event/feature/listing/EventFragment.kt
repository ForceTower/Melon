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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.feature.shared.UFragment
import dev.forcetower.event.databinding.FragmentEventBinding
import org.threeten.bp.ZonedDateTime
import java.util.UUID

class EventFragment : UFragment() {
    private lateinit var binding: FragmentEventBinding
    private lateinit var adapter: EventAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEventBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = EventAdapter()
        binding.recyclerEvents.adapter = adapter
        adapter.submitList(events)
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